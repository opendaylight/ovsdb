package org.opendaylight.ovsdb.internal.jsonrpc;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

public class TestClient extends TestCase {

        String serverurl = "127.0.0.1";
        int serverport = 8080;

        NettyBootStrapper bootstrapper = new NettyBootStrapper();
        JsonRpcDecoder jsonRpcDecoder = new JsonRpcDecoder(100000);

        public void setupServer() throws Exception {
            bootstrapper.startServer(serverport,
                    jsonRpcDecoder,
                    new LoggingHandler(LogLevel.DEBUG));
        }

        public void shutDownServer() throws InterruptedException {
            bootstrapper.stopServer();
        }

        @Test
        public void testBasicFlow() throws Exception {
            setupServer();
            Socket socket = socket = new Socket(serverurl, serverport);

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

    /*
       create and a json of specified size
     */
    private void writeJson(OutputStream outputStream, int times) throws IOException {
        outputStream.write("{".getBytes("UTF-8"));
        for (int i = 0 ; i < times; i ++) {
            counter++;
            String s = ",\"key1"+ counter +"\":\"planet of apes" + counter +
                    "\", \"key2"+ counter +"\":{\"k1\":\"ovs-db rocks the world\"}";
            outputStream.write(s.substring(i == 0 ? 1 : 0).getBytes("UTF-8"));
            System.out.println("data counter = " + counter);
        }
        outputStream.write("}".getBytes("UTF-8"));
    }

    /*
      writes a partial json and flush to simulate the case where netty gets half the message and
      has to frame it accordingly.
     */
    private void writePartialFirst(OutputStream outputStream) throws IOException {
        counter++;
        String s  = "                       {\"part"+ counter+"\":";
        outputStream.write(s.getBytes("UTF-8"));
        System.out.println("partial first half counter = " + counter);
    }

    /*
      finishes the json started by writePartialFirst
     */
    private void writePartialLast(OutputStream outputStream) throws IOException {
        String s  = "\"val"+ counter+"\"}";
        outputStream.write(s.getBytes("UTF-8"));
        System.out.println("partial second half counter = " + counter);
    }

}

