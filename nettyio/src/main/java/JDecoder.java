

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;


public class JDecoder extends FrameDecoder {

    byte[] remainderBytes = null;
    ObjectMapper mapper = new ObjectMapper();
    MappingJsonFactory mappingFactory = new MappingJsonFactory(mapper);

    @Override
    protected Object decode(ChannelHandlerContext chc, Channel channel,
                            ChannelBuffer cbuffer) throws Exception {

        cbuffer.markReaderIndex();
        ChannelBuffer compositeBuffer = cbuffer;
        if (remainderBytes != null) {

            ChannelBuffer remainderBuffer =
                    ChannelBuffers.wrappedBuffer(remainderBytes);
            compositeBuffer =
                    ChannelBuffers.wrappedBuffer(remainderBuffer, cbuffer);
            remainderBytes = null;
        }
        ChannelBufferInputStream cbis = new ChannelBufferInputStream(compositeBuffer);
        JsonParser jp = mappingFactory.createJsonParser(cbis);
        JsonNode node = null;
        try {
            node = jp.readValueAsTree();
        } catch (EOFException e) {

            return null;
        } catch (JsonProcessingException e) {
            if (e.getMessage().contains("end-of-input")) {
                compositeBuffer.resetReaderIndex();

                return null;
            } else {
                throw e;
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        jp.releaseBuffered(os);
        if (os.size() > 0) {
            remainderBytes = os.toByteArray();
        }
        return node;
    }

}

