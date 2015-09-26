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
import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class JsonRpcDecoderTest {
    static int testJson_BYTES = 179;
    String testJson;
    String prettyTestJson;
    static final String PREAMBLE = "                    ";
    static final String PARTIAL_START = "{\"foo\":";
    static final String PARTIAL_END = "{\"bar\":\"baz\"}}";

    JsonRpcDecoder decoder;
    EmbeddedChannel ch;

    @Before
    public void setUp() throws Exception {
        decoder = new JsonRpcDecoder(1000);
        ch = new EmbeddedChannel(decoder);

        URL testJsonUrl = Resources.getResource(JsonRpcDecoderTest.class, "test.json");
        testJson = Resources.toString(testJsonUrl, Charsets.UTF_8);
        URL prettyTestJsoUrl = Resources.getResource(JsonRpcDecoderTest.class, "pretty-test.json");
        prettyTestJson = Resources.toString(prettyTestJsoUrl, Charsets.UTF_8);
    }

    /**
     * Test decoding the Stringified Json text in test.json to
     * individual Json node objects.
     * @throws Exception
     */
    @Test
    public void testDecode() throws Exception {
        for (int i = 0; i < 10; i++) {
            ch.writeInbound(copiedBuffer(testJson, CharsetUtil.UTF_8));
        }
        ch.readInbound();
        assertEquals(10, decoder.getRecordsRead());
        ch.finish();
    }

    /**
     * Test decoding the Stringified Json text in pretty-test.json to
     * individual Json node objects.
     * @throws Exception
     */
    @Test
    public void testDecodePrettyJson() throws Exception {
        ch.writeInbound(copiedBuffer(prettyTestJson, CharsetUtil.UTF_8));
        ch.readInbound();
        assertEquals(1, decoder.getRecordsRead());
        ch.finish();
    }

    /**
     * Test decoding the Stringified Json text with large spaces to
     * individual Json node objects.
     * @throws Exception
     */
    @Test
    public void testDecodeSkipSpaces() throws Exception {
        ch.writeInbound(copiedBuffer(PREAMBLE + testJson + PREAMBLE + testJson, CharsetUtil.UTF_8));
        ch.readInbound();
        assertEquals(2, decoder.getRecordsRead());
        ch.finish();
    }

    /**
     * Test whether phased decoding is allowed with JsonRpcDecoder by
     * writing Json string over two separate iterations, and checking if
     * the decoder collates the record appropriately.
     * @throws Exception
     */
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

    /**
     * Test whether decoder throws appropriate DecoderException when
     * passing a Json string using an unsupported (i.e., UTF-16)
     * character set.
     * @throws Exception
     */
    @Test(expected= DecoderException.class)
    public void testDecodeInvalidEncoding() throws Exception {
        ch.writeInbound(copiedBuffer(testJson, CharsetUtil.UTF_16));
        ch.finish();
    }
    /* Disabling this test as the limit was changed 
     * from exception to a log warning...
    /**
     * Test whether decoder throws appropriate TooLongFrameException
     * when passing a Json string longer than the decoder's maximum
     * frame length.
     * @throws Exception
     */
    /*
    @Test(expected=TooLongFrameException.class)
    public void testDecodeFrameLengthExceed() {
        decoder = new JsonRpcDecoder(testJson_BYTES -1);
        ch = new EmbeddedChannel(decoder);
        ch.writeInbound(copiedBuffer(testJson, CharsetUtil.UTF_8));
        ch.finish();
    }*/
}
