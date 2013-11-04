package org.opendaylight.ovsdb.lib.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Maps;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.netty.channel.Channel;

public class JsonRpcEndpoint {

    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);
    private final ServiceHandler serviceInvoker;

    private ObjectMapper objectMapper;
    private Channel nettyChannel;
    private Map<String, CallContext> methodContext = Maps.newHashMap();

    public JsonRpcEndpoint(ObjectMapper objectMapper, Channel channel, ServiceHandler handler) {
        this.objectMapper = objectMapper;
        this.nettyChannel = channel;
        this.serviceInvoker = handler;
    }

    public <T> T getClient(final Node node, Class<T> klazz) {

        return Reflection.newProxy(klazz, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                JsonRpc10Request request = new JsonRpc10Request(UUID.randomUUID().toString());
                request.setMethod(method.getName());

                if (args != null && args.length != 0) {
                    List<Object> params = null;

                    if (args.length == 1) {
                        if (args[0] instanceof Params) {
                            params = ((Params) args[0]).params();
                        } else if (args[0] instanceof List) {
                            params = (List<Object>) args[0];
                        }

                        if (params == null) {
                            throw new RuntimeException("do not understand this argument yet");
                        }
                        request.setParams(params);
                    }
                }

                String s = objectMapper.writeValueAsString(request);
                logger.debug("{}", s);

                SettableFuture<Object> sf = SettableFuture.create();
                methodContext.put(request.getId(), new CallContext(request, method, sf));

                nettyChannel.writeAndFlush(s);

                return sf;
            }
        }
        );
    }


    public void processResult(JsonNode response) throws NoSuchMethodException {

        CallContext returnCtxt = methodContext.get(response.get("id").asText());
        if (returnCtxt == null) return;

        if (ListenableFuture.class == returnCtxt.getMethod().getReturnType()) {
            TypeToken<?> retType = TypeToken.of(
                    returnCtxt.getMethod().getGenericReturnType())
                    .resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
            JavaType javaType =  TypeFactory.defaultInstance().constructType (retType.getType());

            JsonNode result = response.get("result");
            logger.debug("Response : {}", result.toString());

            Object result1 = objectMapper.convertValue(result, javaType);
            JsonNode error = response.get("error");
            if (error != null) {
                logger.debug("Error : {}", error.toString());
            }

            returnCtxt.getFuture().set(result1);

        } else {
            throw new RuntimeException("donno how to deal with this");
        }
    }

    public void processRequest(Node node, JsonNode requestJson) {
        JsonRpc10Request request = new JsonRpc10Request(requestJson.get("id").asText());
        request.setMethod(requestJson.get("method").asText());
        logger.debug("Request : {} {}", requestJson.get("method"), requestJson.get("params"));

        JsonRpc10Response response = new JsonRpc10Response(request.getId());
        try {
            List params = serviceInvoker.handleCall(request.getMethod(), requestJson.get("params"));
            response.setResult(params);
        } catch (Exception e) {
            response.setError(null);
        }

        try {
            String s = objectMapper.writeValueAsString(response);
            nettyChannel.writeAndFlush(s);
        } catch (JsonProcessingException e) {
            logger.error("unknown exception", e);
            //todo: we need to close the channel when this happens
            throw new RuntimeException(e);
        }
    }

    public Map<String, CallContext> getMethodContext() {
        return methodContext;
    }

    public class CallContext {
        Method method;
        JsonRpc10Request request;
        SettableFuture<Object> future;

        public CallContext(JsonRpc10Request request, Method method, SettableFuture<Object> future) {
            this.method = method;
            this.request = request;
            this.future = future;
        }

        public Method getMethod() {
            return method;
        }

        public JsonRpc10Request getRequest() {
            return request;
        }

        public SettableFuture<Object> getFuture() {
            return future;
        }
    }
}
