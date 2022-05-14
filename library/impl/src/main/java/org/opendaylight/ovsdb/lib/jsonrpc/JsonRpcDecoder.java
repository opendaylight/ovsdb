/*
 * Copyright (c) 2013, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.jsonrpc;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.opendaylight.ovsdb.lib.error.InvalidEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON RPC 1.0 compatible decoder capable of decoding JSON messages from a TCP stream.
 * The stream is framed first by inspecting the json for valid end marker (left curly)
 * and is passed to a Json parser (jackson) for converting into an object model.
 *
 * <p>There are no JSON parsers that I am aware of that does non blocking parsing.
 * This approach avoids having to run json parser over and over again on the entire
 * stream waiting for input. Parser is invoked only when we know of a full JSON message
 * in the stream.
 */
public class JsonRpcDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcDecoder.class);
    private static final JsonFactory JSON_FACTORY = new MappingJsonFactory();

    private final int maxFrameLength;
    //Indicates if the frame limit warning was issued
    private boolean maxFrameLimitWasReached = false;

    private final IOContext jacksonIOContext = new IOContext(new BufferRecycler(), (Object) null, false);

    // context for the previously read incomplete records
    private int lastRecordBytes = 0;
    private int leftCurlies = 0;
    private int rightCurlies = 0;
    private boolean inS = false;

    private int recordsRead;

    public JsonRpcDecoder(final int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf buf, final List<Object> out)
            throws IOException {
        LOG.trace("readable bytes {}, records read {}, incomplete record bytes {}", buf.readableBytes(),
            recordsRead, lastRecordBytes);

        if (lastRecordBytes == 0) {
            if (buf.readableBytes() < 4) {
                return; //wait for more data
            }

            skipSpaces(buf);

            byte[] buff = new byte[4];
            buf.getBytes(buf.readerIndex(), buff);
            ByteSourceJsonBootstrapper strapper = new ByteSourceJsonBootstrapper(jacksonIOContext, buff, 0, 4);
            JsonEncoding jsonEncoding = strapper.detectEncoding();
            if (!JsonEncoding.UTF8.equals(jsonEncoding)) {
                throw new InvalidEncodingException(jsonEncoding.getJavaName(), "currently only UTF-8 is supported");
            }
        }

        int index = lastRecordBytes + buf.readerIndex();

        for (; index < buf.writerIndex(); index++) {
            switch (buf.getByte(index)) {
                case '{':
                    if (!inS) {
                        leftCurlies++;
                    }
                    break;
                case '}':
                    if (!inS) {
                        rightCurlies++;
                    }
                    break;
                case '"':
                    if (buf.getByte(index - 1) != '\\') {
                        inS = !inS;
                    }
                    break;
                default:
                    break;
            }

            if (leftCurlies != 0 && leftCurlies == rightCurlies && !inS) {
                ByteBuf slice = buf.readSlice(1 + index - buf.readerIndex());
                JsonParser jp = JSON_FACTORY.createParser((InputStream) new ByteBufInputStream(slice));
                JsonNode root = jp.readValueAsTree();
                out.add(root);
                leftCurlies = 0;
                rightCurlies = 0;
                lastRecordBytes = 0;
                recordsRead++;
                break;
            }

            /*
             * Changing this limit to being a warning, we do not wish to "break" in scale environment
             * and currently this limits the ovs of having only around 50 ports defined...
             * I do acknowledge the fast that this might be risky in case of huge amount of strings
             * in which the controller can crash with an OOM, however seems that we need a really huge
             * ovs to reach that limit.
             */

            //We do not want to issue a log message on every extent of the buffer
            //hence logging only once
            if (index - buf.readerIndex() >= maxFrameLength && !maxFrameLimitWasReached) {
                maxFrameLimitWasReached = true;
                LOG.warn("***** OVSDB Frame limit of {} bytes has been reached! *****", this.maxFrameLength);
            }
        }

        // end of stream, save the incomplete record index to avoid reexamining the whole on next run
        if (index >= buf.writerIndex()) {
            lastRecordBytes = buf.readableBytes();
        }
    }

    public int getRecordsRead() {
        return recordsRead;
    }

    private static void skipSpaces(final ByteBuf byteBuf) throws IOException {
        while (byteBuf.isReadable()) {
            int ch = byteBuf.getByte(byteBuf.readerIndex()) & 0xFF;
            if (!(ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t')) {
                return;
            } else {
                byteBuf.readByte(); //move the read index
            }
        }
    }
}
