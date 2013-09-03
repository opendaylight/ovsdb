

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.string.StringEncoder;


public class IClientPipelineFactory implements ChannelPipelineFactory {


    @Override
    public ChannelPipeline getPipeline() throws Exception {
        JDecoder jsonRpcDecoder = new JDecoder();


        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("jsondecoder", jsonRpcDecoder);
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("handler", new ClientHandler());
        return pipeline;
    }
}
