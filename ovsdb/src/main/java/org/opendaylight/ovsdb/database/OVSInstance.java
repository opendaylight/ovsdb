package org.opendaylight.ovsdb.database;


import io.netty.channel.Channel;

import org.opendaylight.ovsdb.internal.Connection;
import org.opendaylight.ovsdb.internal.MessageHandler;
import org.opendaylight.ovsdb.internal.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class OVSInstance {
    static final Class returnType = Map.class;
    private String uuid;
    private static final Logger logger = LoggerFactory.getLogger(OVSInstance.class);
    public OVSInstance(){
        this.uuid = null;
    }

    public OVSInstance(String uuid){
        this.uuid = uuid;
    }

    @SuppressWarnings("unchecked")
    public static OVSInstance monitorOVS(Connection connection){
        String request = "{\"method\":\"monitor\",\"params\":[\"Open_vSwitch\",null,{\"Open_vSwitch\":{\"columns\":[\"_uuid\",\"bridges\"]}}],\"id\":" + connection.getIdCounter()+ "}";
        try{

            Long id = connection.getIdCounter();
            MessageMapper.getMapper().map(id, Map.class);
            connection.sendMessage(request);
            Channel channel = connection.getChannel();
            MessageHandler handler = (MessageHandler) channel.pipeline().get("messageHandler");
            Future<Object> future = handler.getResponse(id);
            if (future != null) {
                Map<String, Object> response = (HashMap) future.get();
                Map<String, Object> ovsTable = (HashMap) response.get("Open_vSwitch");
                if(ovsTable != null){
                    String uuid = (String) ovsTable.keySet().toArray()[0];
                    return new OVSInstance(uuid);
                }
            } else {

                logger.error("Future is dark");
            }
        } catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public String getUuid(){
        return this.uuid;
    }

}
