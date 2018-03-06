/*
 * Copyright © 2013, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.opendaylight.ovsdb.lib.error.UnexpectedResultException;
import org.opendaylight.ovsdb.lib.error.UnsupportedArgumentException;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcEndpoint.class);
    private static final int REAPER_THREADS = 3;
    private static final ThreadFactory FUTURE_REAPER_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("OVSDB-Lib-Future-Reaper-%d")
            .setDaemon(true).build();
    private static final ScheduledExecutorService FUTURE_REAPER_SERVICE
            = Executors.newScheduledThreadPool(REAPER_THREADS, FUTURE_REAPER_THREAD_FACTORY);

    private static int reaperInterval = 1000;

    public static class CallContext {
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
    Map<String, CallContext> methodContext = new ConcurrentHashMap<>();
    Map<Object, OvsdbRPC.Callback> requestCallbacks = new HashMap<>();

    public JsonRpcEndpoint(ObjectMapper objectMapper, Channel channel) {
        this.objectMapper = objectMapper;
        this.nettyChannel = channel;
    }

    public <T> T getClient(final Object context, Class<T> klazz) {

        return Reflection.newProxy(klazz, (proxy, method, args) -> {
            if (method.getName().equals(OvsdbRPC.REGISTER_CALLBACK_METHOD)) {
                if (args == null || args.length != 1 || !(args[0] instanceof OvsdbRPC.Callback)) {
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
            LOG.trace("getClient Request : {}", requestString);

            SettableFuture<Object> sf = SettableFuture.create();
            methodContext.put(request.getId(), new CallContext(request, method, sf));
            FUTURE_REAPER_SERVICE.schedule(() -> {
                CallContext cc = methodContext.remove(request.getId());
                if (cc != null) {
                    if (cc.getFuture().isDone() || cc.getFuture().isCancelled()) {
                        return;
                    }
                    cc.getFuture().cancel(false);
                }
            }, reaperInterval, TimeUnit.MILLISECONDS);

            nettyChannel.writeAndFlush(requestString);

            return sf;
        }
        );
    }

    public void processResult(JsonNode response) throws NoSuchMethodException {

        LOG.trace("Response : {}", response.toString());
        CallContext returnCtxt = methodContext.remove(response.get("id").asText());
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
                LOG.error("Error : {}", error.toString());
            }

            returnCtxt.getFuture().set(result1);

        } else {
            throw new UnexpectedResultException("Don't know how to handle this");
        }
    }

    public void processRequest(Object context, JsonNode requestJson) {
        JsonRpc10Request request = new JsonRpc10Request(requestJson.get("id").asText());
        request.setMethod(requestJson.get("method").asText());
        LOG.trace("Request : {} {} {}", requestJson.get("id"), requestJson.get("method"),
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
                        LOG.error("Unable to invoke callback {}", method.getName(), e);
                    }
                    return;
                }
            }
        }

        // Echo dont need any special processing. hence handling it internally.

        if (request.getMethod().equals("echo")) {
            JsonRpc10Response response = new JsonRpc10Response(request.getId());
            response.setError(null);
            try {
                String jsonString = objectMapper.writeValueAsString(response);
                nettyChannel.writeAndFlush(jsonString);
            } catch (JsonProcessingException e) {
                LOG.error("Exception while processing JSON response {}", response, e);
            }
            return;
        }

        // send a null response for list_dbs
        if (request.getMethod().equals("list_dbs")) {
            JsonRpc10Response response = new JsonRpc10Response(request.getId());
            response.setError(null);
            try {
                String jsonString = objectMapper.writeValueAsString(response);
                nettyChannel.writeAndFlush(jsonString);
            } catch (JsonProcessingException e) {
                LOG.error("Exception while processing JSON response {}", response, e);
            }
            return;
        }

        LOG.error("No handler for Request : {} on {}", requestJson.toString(), context);
    }

    public Map<String, CallContext> getMethodContext() {
        return methodContext;
    }

    public static void setReaperInterval(int interval) {
        reaperInterval = interval;
        LOG.info("Ovsdb Rpc Task interval is set to {} millisecond", reaperInterval);
    }

    public static void close() {
        LOG.info("Shutting down reaper executor service");
        FUTURE_REAPER_SERVICE.shutdownNow();
    }
}
