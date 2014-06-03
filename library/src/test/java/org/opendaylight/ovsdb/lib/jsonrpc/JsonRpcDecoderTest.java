/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.lib.jsonrpc;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.junit.Assert.*;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import org.junit.Before;
import org.junit.Test;

public class JsonRpcDecoderTest {
    static int TEST_JSON_BYTES = 179;
    static final String TEST_JSON = "{\"Image\":{\"Width\":800,\"Height\":600,\"Title\":\"View from 15th Floor\",\"Thumbnail\":{\"Url\":\"http://www.example.com/image/481989943\",\"Height\":125,\"Width\":\"100\"},\"IDs\":[116,943,234,38793]}}";
    static final String PREAMBLE = "                    ";
    static final String PRETTY_TEST_JSON = "{\n" +
            "\t\"Image\": {\n" +
            "\t\t\"Width\": 800,\n" +
            "\t\t\"Height\": 600,\n" +
            "\t\t\"Title\": \"View from 15th Floor\",\n" +
            "\t\t\"Thumbnail\": {\n" +
            "\t\t\t\"Url\": \"http://www.example.com/image/481989943\",\n" +
            "\t\t\t\"Height\": 125,\n" +
            "\t\t\t\"Width\": \"100\"\n" +
            "\t\t},\n" +
            "\t\t\"IDs\": [\n" +
            "\t\t116,\n" +
            "\t\t943,\n" +
            "\t\t234,\n" +
            "\t\t38793\n" +
            "\t\t]\n" +
            "\t}\n" +
            "}";
    static final String PARTIAL_START = "{\"foo\":";
    static final String PARTIAL_END = "{\"bar\":\"baz\"}}";

    JsonRpcDecoder decoder;
    EmbeddedChannel ch;

    @Before
    public void setUp() throws Exception {
        decoder = new JsonRpcDecoder(1000);
        ch = new EmbeddedChannel(decoder);
    }

    @Test
    public void testDecode() throws Exception {
        for (int i = 0; i < 10; i++) {
            ch.writeInbound(copiedBuffer(TEST_JSON, CharsetUtil.UTF_8));
        }
        ch.readInbound();
        assertEquals(10, decoder.getRecordsRead());
        ch.finish();
    }

    @Test
    public void testDecodePrettyJson() throws Exception {
        ch.writeInbound(copiedBuffer(PRETTY_TEST_JSON, CharsetUtil.UTF_8));
        ch.readInbound();
        assertEquals(1, decoder.getRecordsRead());
        ch.finish();
    }

    @Test
    public void testDecodeSkipSpaces() throws Exception {
        ch.writeInbound(copiedBuffer(PREAMBLE + TEST_JSON + PREAMBLE + TEST_JSON, CharsetUtil.UTF_8));
        ch.readInbound();
        assertEquals(2, decoder.getRecordsRead());
        ch.finish();
    }

    @Test
    public void testDecodePartial() throws Exception {
        ch.writeInbound(copiedBuffer(PARTIAL_START, CharsetUtil.UTF_8));
        ch.readInbound();
        Thread.sleep(10);
        ch.writeInbound(copiedBuffer(PARTIAL_END, CharsetUtil.UTF_8));
        ch.readInbound();
        assertEquals(1, decoder.getRecordsRead());
        ch.finish();
    }

    @Test(expected= DecoderException.class)
    public void testDecodeInvalidEncoding() throws Exception {
        ch.writeInbound(copiedBuffer(TEST_JSON, CharsetUtil.UTF_16));
        ch.finish();
    }

    @Test(expected=TooLongFrameException.class)
    public void testDecodeFrameLengthExceed() {
        decoder = new JsonRpcDecoder(TEST_JSON_BYTES -1);
        ch = new EmbeddedChannel(decoder);
        ch.writeInbound(copiedBuffer(TEST_JSON, CharsetUtil.UTF_8));
        ch.finish();
    }
}