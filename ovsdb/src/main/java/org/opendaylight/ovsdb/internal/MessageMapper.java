package org.opendaylight.ovsdb.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MessageMapper {

    private static final Logger logger = LoggerFactory.getLogger(MessageMapper.class);

    private static MessageMapper mapper = null;
    Map<Long, Class<?>> responseMapper = new HashMap<Long, Class<?>>();
    Map<String, Class<?>> requestMapper = new HashMap<String, Class<?>>();

    private MessageMapper() {
    }

    public static MessageMapper getMapper() {
        if (mapper == null) mapper = new MessageMapper();
        return mapper;
    }

    public void map(long id, Class<?> rClass) {
        responseMapper.put(Long.valueOf(id), rClass);
    }

    public Class<?> pop(long id) {
        return responseMapper.remove(id);
    }

    public void map(String type, Class<?> rClass) {
        requestMapper.put(type, rClass);
    }

    public Class<?> get(String type) {
        return requestMapper.get(type);
    }
}
