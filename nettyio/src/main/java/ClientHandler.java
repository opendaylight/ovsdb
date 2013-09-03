

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

//public class ClientHandler extends SimpleChannelUpstreamHandler {
public class ClientHandler extends SimpleChannelHandler {


    private static final Logger log = getLogger(ClientHandler.class);
    private final ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        System.out.println("--------------------");
        System.out.println(e.toString());
        System.out.println("--------------------");

        JsonNode jsonparse = (JsonNode)e.getMessage();
        ObjectMapper mapper = new ObjectMapper();
      //  EchoReplyPojo rep = mapper.treeToValue(jn, EchoReplyPojo.class);
        System.out.println(jsonparse.get("method"));
       // System.out.println("JSONNODE --> " + rep);
        String echoReply = "{ \"result\": [], \"id\": \"echo\"} ";
        String jmethod = String.valueOf(jsonparse.get("method"));
        System.out.println("METHOD--->" + jmethod);
        if (jmethod.contains("echo")){
            EchoReplyPojo echoreply = new EchoReplyPojo();
            echoreply.setId("echo");

//            byte[] message = echoReply.getBytes("UTF-8");
//            ChannelBuffer buf = ChannelBuffers.buffer(message.length + 2);
//            buf.clear();
//            short len = (short)message.length;
//            buf.writeShort(len);
//            buf.writeBytes(message);
//            e.getChannel().write(buf + "\\r\\n\"");
            ChannelFuture future = e.getChannel().write(echoReply);

           // String ereply = mapper.writeValueAsString(echoreply);



//            TreeNode jn = null;
//            EchoReplyPojo echoreply = mapper.treeToValue(jn, EchoReplyPojo.class);
//            System.out.println("ECHO METHOD--->" + jn);
        }


//        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
//        int size = buffer.readableBytes();
//        byte[] bytes = new byte[size];
//        buffer.readBytes(bytes);
//        log.info("Client Returned: " + new String(bytes));
//        String jstring = new String(bytes);
//        GpojoEchoReply inputJson = new Gson().fromJson(jstring, GpojoEchoReply.class);
//        System.out.println(inputJson.getResult());
//        System.out.println(inputJson.getId());

        JsonObject responseObj = new JsonObject();

        System.out.println("Empty Response? --> " + responseObj.toString());


        ////////////////////

  //   EchoReplyPojo echoreply = new EchoReplyPojo();
        EchoReplyPojo echoreply = new EchoReplyPojo();
        EchoRequestPojo echorequest = new EchoRequestPojo();

        echorequest.setMethod("echo");
        echorequest.setId("echo");
        echoreply.setId("echo");

        String erequest = mapper.writeValueAsString(echorequest);
        String ereply = mapper.writeValueAsString(echoreply);
//        System.out.println(erequest);
//        System.out.println(ereply);


///////////////////////////////////////

        if (e.getMessage() instanceof EchoNullReplyPojo) {
            System.out.println("MATCH");
            EchoRequestPojo pojo = (EchoRequestPojo) e.getMessage();

            e.getChannel().write(ereply);
            System.out.println("Sending Reply");
        }
        if (e.getMessage() instanceof EchoNullReplyPojo) {
            System.out.println("MATCH");
            EchoRequestPojo pojo = (EchoRequestPojo) e.getMessage();

            e.getChannel().write(ereply);
            System.out.println("Sending Reply");
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logExceptionEventAndClose(e);
    }

    private void logExceptionEventAndClose(ExceptionEvent e) {
        final Throwable throwable = e.getCause();
        log.error("Oh Noes!: " + throwable, throwable);
        Channel ch = e.getChannel();
        ch.close();
    }
}


//JsonNode jn = (JsonNode)e.getMessage();
//String echoReply = "{ \"result\": [], \"id\": \"echo\"} ";
//        System.out.println(jn.toString());


//        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
//        int size = buffer.readableBytes();
//        byte[] bytes = new byte[size];
//        buffer.readBytes(bytes);
//        log.info("Client Returned: " + new String(bytes));
//        String jstring = new String(bytes);
//        GpojoEchoReply inputJson = new Gson().fromJson(jstring, GpojoEchoReply.class);
//        System.out.println(inputJson.getResult());
//        System.out.println(inputJson.getId());
//        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
//        StringBuilder st = new StringBuilder();
//        while (buf.readable()){
//            //System.out.println((char) buf.readByte());
//            st.append((char) buf.readByte());
//            //System.out.flush();
//        }
//        System.out.println("Return:: " + st);
//        System.out.flush();
//
//        }
// ChannelBuffer compositeBuffer = cb;
//        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
//        byte[] remainderBytes = null;
//        if (remainderBytes != null) {
//
//            ChannelBuffer remainderBuffer =
//                    ChannelBuffers.wrappedBuffer(remainderBytes);
//            compositeBuffer =
//                    ChannelBuffers.wrappedBuffer(remainderBuffer, cb);
//        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
//        ByteArrayInputStream bis = new ByteArrayInputStream(buffer.array());
//        DataInputStream dis = new DataInputStream(bis);
//        System.out.println(dis);
//
//        {
//            ChannelBuffer buf = (ChannelBuffer) e.getMessage();
//
//            BigEndianHeapChannelBuffer msg = (BigEndianHeapChannelBuffer) e.getMessage();
//            System.out.println(msg.toString());
//                ChannelBuffer buf = (ChannelBuffer) e.getMessage();
//        StringBuilder st = new StringBuilder();
//        while (buf.readable()){
//            //System.out.println((char) buf.readByte());
//            st.append((char) buf.readByte());
//            //System.out.flush();
//        }
//        System.out.println("Return:: " + st);
//        System.out.flush();

//        final ChannelBuffer requestBuffer = ((ChannelBuffer) e.getMessage()).copy();
//        Channel ch;
//        ch = ctx.getChannel();



