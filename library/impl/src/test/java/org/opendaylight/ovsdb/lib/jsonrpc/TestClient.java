/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;
import org.junit.Test;

public class TestClient extends TestCase {

    String serverurl = "127.0.0.1";
    NettyBootStrapper bootstrapper = new NettyBootStrapper();
    JsonRpcDecoder jsonRpcDecoder = new JsonRpcDecoder(100000);

    public void setupServer() throws Exception {
        bootstrapper.startServer(0, jsonRpcDecoder, new LoggingHandler(LogLevel.DEBUG));
    }

    public void shutDownServer() throws InterruptedException {
        bootstrapper.stopServer();
    }

    /**
     * Testing appropriate ChannelHandler integration for JsonRpcDecoder, so that JSON strings written using an
     * OutputStream connected to a ServerSocket of a Netty ServerBootstrap can be decoded properly.
     */
    @Test
    public void testBasicFlow() throws Exception {
        setupServer();
        Socket socket = new Socket(serverurl, bootstrapper.getServerPort());
        OutputStream outputStream = socket.getOutputStream();

        int records = 20;

        for (int i = 0; i < records; i++) {
            writeJson(outputStream, 1);
            writePartialFirst(outputStream);
            outputStream.flush();
            Thread.sleep(10);
            writePartialLast(outputStream);
        }
        socket.close();
        shutDownServer();

        assertEquals("mismatch in records processed", records * 2, jsonRpcDecoder.getRecordsRead());
    }

    static int counter = 0;

    /**
     * Create and write a json string for specified number of times.
     */
    private static void writeJson(final OutputStream outputStream, final int times) throws IOException {
        outputStream.write("{".getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < times; i++) {
            counter++;
            String string = ",\"key1" + counter + "\":\"planet of apes" + counter + "\", \"key2" + counter
                    + "\":{\"k1\":\"ovs-db rocks the world\"}";
            outputStream.write(string.substring(i == 0 ? 1 : 0).getBytes(StandardCharsets.UTF_8));
        }
        outputStream.write("}".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Writes a partial JSON and flush to simulate the case where netty gets half the message and has to frame it
     * accordingly.
     */
    private static void writePartialFirst(final OutputStream outputStream) throws IOException {
        counter++;
        String string = "                       {\"part" + counter + "\":";
        outputStream.write(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Finishes the JSON started by writePartialFirst.
     */
    private static void writePartialLast(final OutputStream outputStream) throws IOException {
        String string = "\"val" + counter + "\"}";
        outputStream.write(string.getBytes(StandardCharsets.UTF_8));
    }

}
