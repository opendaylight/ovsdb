package org.opendaylight.ovsdb.lib.jsonrpc;

import io.netty.channel.Channel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class JsonRpcEndpoint {

    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

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

    ObjectMapper objectMapper;
    Channel nettyChannel;
    Map<String, CallContext> methodContext = Maps.newHashMap();
    Map<Node, OvsdbRPC.Callback> requestCallbacks = Maps.newHashMap();

    public JsonRpcEndpoint(ObjectMapper objectMapper, Channel channel) {
        this.objectMapper = objectMapper;
        this.nettyChannel = channel;
    }

    public <T> T getClient(final Node node, Class<T> klazz) {

        return Reflection.newProxy(klazz, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals(OvsdbRPC.REGISTER_CALLBACK_METHOD)) {
                    if ((args == null) || args.length != 1 || !(args[0] instanceof OvsdbRPC.Callback)) return false;
                    requestCallbacks.put(node, (OvsdbRPC.Callback)args[0]);
                    return true;
                }

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
                logger.trace("{}", s);

                SettableFuture<Object> sf = SettableFuture.create();
                methodContext.put(request.getId(), new CallContext(request, method, sf));

                nettyChannel.writeAndFlush(s);

                return sf;
            }
        }
        );
    }

    public void processResult(JsonNode response) throws NoSuchMethodException {

        logger.trace("Response : {}", response.toString());
        CallContext returnCtxt = methodContext.get(response.get("id").asText());
        if (returnCtxt == null) return;

        if (ListenableFuture.class == returnCtxt.getMethod().getReturnType()) {
            TypeToken<?> retType = TypeToken.of(
                    returnCtxt.getMethod().getGenericReturnType())
                    .resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
            JavaType javaType =  TypeFactory.defaultInstance().constructType (retType.getType());

            JsonNode result = response.get("result");
            logger.trace("Result : {}", result.toString());

            Object result1 = objectMapper.convertValue(result, javaType);
            JsonNode error = response.get("error");
            if (error != null && !error.isNull()) {
                logger.error("Error : {}", error.toString());
            }

            returnCtxt.getFuture().set(result1);

        } else {
            throw new RuntimeException("donno how to deal with this");
        }
    }

    public void processRequest(Node node, JsonNode requestJson) {
        JsonRpc10Request request = new JsonRpc10Request(requestJson.get("id").asText());
        request.setMethod(requestJson.get("method").asText());
        logger.trace("Request : {} {}", requestJson.get("method"), requestJson.get("params"));
        OvsdbRPC.Callback callback = requestCallbacks.get(node);
        if (callback != null) {
            Method[] methods = callback.getClass().getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(request.getMethod())) {
                    Class<?>[] parameters = m.getParameterTypes();
                    JsonNode params = requestJson.get("params");
                    Object param = objectMapper.convertValue(params, parameters[1]);
                    try {
                        m.invoke(callback, node, param);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.error("Unable to invoke callback " + m.getName(), e);
                    }
                    return;
                }
            }
        }

        // Echo dont need any special processing. hence handling it internally.

        if (request.getMethod().equals("echo")) {
            JsonRpc10Response response = new JsonRpc10Response(request.getId());
            response.setError(null);
            String s = null;
            try {
                s = objectMapper.writeValueAsString(response);
                nettyChannel.writeAndFlush(s);
            } catch (JsonProcessingException e) {
                logger.error("Exception while processing JSON string " + s, e );
            }
            return;
        }

        logger.error("No handler for Request : {} on {}",requestJson.toString(), node);
    }

    public Map<String, CallContext> getMethodContext() {
        return methodContext;
    }
}
