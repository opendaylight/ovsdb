/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import io.netty.channel.Channel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.opendaylight.ovsdb.lib.error.UnexpectedResultException;
import org.opendaylight.ovsdb.lib.error.UnsupportedArgumentException;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;
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
    Map<Object, OvsdbRPC.Callback> requestCallbacks = Maps.newHashMap();

    public JsonRpcEndpoint(ObjectMapper objectMapper, Channel channel) {
        this.objectMapper = objectMapper;
        this.nettyChannel = channel;
    }

    public <T> T getClient(final Object context, Class<T> klazz) {

        return Reflection.newProxy(klazz, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals(OvsdbRPC.REGISTER_CALLBACK_METHOD)) {
                    if ((args == null) || args.length != 1 || !(args[0] instanceof OvsdbRPC.Callback)) {
                        return false;
                    }
                    requestCallbacks.put(context, (OvsdbRPC.Callback)args[0]);
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
                            throw new UnsupportedArgumentException("do not understand this argument yet");
                        }
                        request.setParams(params);
                    }
                }

                String requestString = objectMapper.writeValueAsString(request);
                logger.debug("getClient Request : {}", requestString);

                SettableFuture<Object> sf = SettableFuture.create();
                methodContext.put(request.getId(), new CallContext(request, method, sf));

                nettyChannel.writeAndFlush(requestString);

                return sf;
            }
        }
        );
    }

    public void processResult(JsonNode response) throws NoSuchMethodException {

        logger.trace("Response : {}", response.toString());
        CallContext returnCtxt = methodContext.get(response.get("id").asText());
        if (returnCtxt == null) {
            return;
        }

        if (ListenableFuture.class == returnCtxt.getMethod().getReturnType()) {
            TypeToken<?> retType = TypeToken.of(
                    returnCtxt.getMethod().getGenericReturnType())
                    .resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
            JavaType javaType =  TypeFactory.defaultInstance().constructType(retType.getType());

            JsonNode result = response.get("result");
            Object result1 = objectMapper.convertValue(result, javaType);
            JsonNode error = response.get("error");
            if (error != null && !error.isNull()) {
                logger.error("Error : {}", error.toString());
            }

            returnCtxt.getFuture().set(result1);

        } else {
            throw new UnexpectedResultException("Don't know how to handle this");
        }
    }

    public void processRequest(Object context, JsonNode requestJson) {
        JsonRpc10Request request = new JsonRpc10Request(requestJson.get("id").asText());
        request.setMethod(requestJson.get("method").asText());
        logger.debug("Request : {} {} {}", requestJson.get("id"), requestJson.get("method"),
                requestJson.get("params"));
        OvsdbRPC.Callback callback = requestCallbacks.get(context);
        if (callback != null) {
            Method[] methods = callback.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(request.getMethod())) {
                    Class<?>[] parameters = method.getParameterTypes();
                    JsonNode params = requestJson.get("params");
                    Object param = objectMapper.convertValue(params, parameters[1]);
                    try {
                        Invokable from = Invokable.from(method);
                        from.setAccessible(true);
                        from.invoke(callback, context, param);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.error("Unable to invoke callback " + method.getName(), e);
                    }
                    return;
                }
            }
        }

        // Echo dont need any special processing. hence handling it internally.

        if (request.getMethod().equals("echo")) {
            JsonRpc10Response response = new JsonRpc10Response(request.getId());
            response.setError(null);
            String jsonString = null;
            try {
                jsonString = objectMapper.writeValueAsString(response);
                nettyChannel.writeAndFlush(jsonString);
            } catch (JsonProcessingException e) {
                logger.error("Exception while processing JSON string " + jsonString, e );
            }
            return;
        }

        // send a null response for list_dbs
        if (request.getMethod().equals("list_dbs")) {
            JsonRpc10Response response = new JsonRpc10Response(request.getId());
            response.setError(null);
            String jsonString = null;
            try {
                jsonString = objectMapper.writeValueAsString(response);
                nettyChannel.writeAndFlush(jsonString);
            } catch (JsonProcessingException e) {
                logger.error("Exception while processing JSON string " + jsonString, e );
            }
            return;
        }

        logger.error("No handler for Request : {} on {}",requestJson.toString(), context);
        return;
    }

    public Map<String, CallContext> getMethodContext() {
        return methodContext;
    }
}
