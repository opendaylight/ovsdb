/*
 * Copyright © 2013, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.opendaylight.ovsdb.lib.error.UnsupportedArgumentException;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.Response;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcEndpoint extends ChannelInboundHandlerAdapter implements OvsdbRPC {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcEndpoint.class);
    private static final int REAPER_THREADS = 3;
    private static final ThreadFactory FUTURE_REAPER_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("OVSDB-Lib-Future-Reaper-%d")
            .setDaemon(true).build();
    private static final ScheduledExecutorService FUTURE_REAPER_SERVICE
            = Executors.newScheduledThreadPool(REAPER_THREADS, FUTURE_REAPER_THREAD_FACTORY);

    private static final JavaType JT_OBJECT = TypeFactory.defaultInstance().constructType(Object.class);
    private static final JavaType JT_JSON_NODE = TypeFactory.defaultInstance().constructType(JsonNode.class);
    private static final JavaType JT_LIST_JSON_NODE = TypeFactory.defaultInstance().constructParametricType(
        List.class, JsonNode.class);
    private static final JavaType JT_LIST_STRING = TypeFactory.defaultInstance().constructParametricType(
        List.class, String.class);

    private static int reaperInterval = 1000;

    private static final class CallContext {
        final JavaType resultType;
        final SettableFuture future;

        CallContext(final JavaType resultType, final SettableFuture future) {
            this.resultType = resultType;
            this.future = future;
        }
    }

    private final Map<String, CallContext> methodContext = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Channel nettyChannel;

    private volatile Callback currentCallback = null;

    public JsonRpcEndpoint(final ObjectMapper objectMapper, final Channel channel) {
        this.objectMapper = requireNonNull(objectMapper);
        this.nettyChannel = requireNonNull(channel);
    }

    // FIXME: the reaper service should probably be split out
    public static void setReaperInterval(final int interval) {
        reaperInterval = interval;
        LOG.info("Ovsdb Rpc Task interval is set to {} millisecond", reaperInterval);
    }

    public static void close() {
        LOG.info("Shutting down reaper executor service");
        FUTURE_REAPER_SERVICE.shutdownNow();
    }

    @Override
    public ListenableFuture<JsonNode> get_schema(final List<String> dbNames) {
        return sendRequest(JT_JSON_NODE, "get_schema", dbNames);
    }

    @Override
    public ListenableFuture<List<String>> echo() {
        return sendRequest(JT_LIST_STRING, "echo");
    }

    @Override
    public ListenableFuture<JsonNode> monitor(final Params equest) {
        return sendRequest(JT_JSON_NODE, "monitor", equest);
    }

    @Override
    public ListenableFuture<List<String>> list_dbs() {
        return sendRequest(JT_LIST_STRING, "list_dbs");
    }

    @Override
    public ListenableFuture<List<JsonNode>> transact(final TransactBuilder transact) {
        return sendRequeast(JT_LIST_JSON_NODE, "transact", transact);
    }

    @Override
    public ListenableFuture<Response> cancel(final String id) {
        // FIXME: reflection-based access did not handle this, this keeps equivalent functionality
        throw new UnsupportedArgumentException("do not understand this argument yet");
    }

    @Override
    public ListenableFuture<JsonNode> monitor_cancel(final Params jsonValue) {
        return sendRequest(JT_JSON_NODE, "monitor_cancel", jsonValue);
    }

    @Override
    public ListenableFuture<Object> lock(final List<String> id) {
        return sendRequest(JT_OBJECT, "lock", id);
    }

    @Override
    public ListenableFuture<Object> steal(final List<String> id) {
        return sendRequest(JT_OBJECT, "steal", id);
    }

    @Override
    public ListenableFuture<Object> unlock(final List<String> id) {
        return sendRequest(JT_OBJECT, "unlock", id);
    }

    @Override
    public boolean registerCallback(final Callback callback) {
        if (callback == null) {
            return false;
        }
        this.currentCallback = callback;
        return true;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof JsonNode)) {
            LOG.debug("Unexpected message {}, closing channel {}", msg, nettyChannel);
            ctx.channel().close();
            return;
        }

        final JsonNode jsonNode = (JsonNode) msg;
        final JsonNode result = jsonNode.get("result");
        if (result != null) {
            handleResponse(jsonNode, result);
            return;
        }
        final JsonNode method = jsonNode.get("method");
        if (method != null && !method.isNull()) {
            handleRequest(jsonNode, method);
            return;
        }

        LOG.debug("Ignoring message {} on channel {}", jsonNode, nettyChannel);
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleRequest(final JsonNode jsonRequest, final JsonNode jsonMethod) {
        final JsonNode id = jsonRequest.get("id");
        final JsonNode params = jsonRequest.get("params");
        if (id == null) {
            LOG.debug("Ignoring request with non-existent id field: {} {}", jsonMethod, params);
            return;
        }

        final String requestId = id.asText();
        if (Strings.isNullOrEmpty(requestId)) {
            LOG.debug("Ignoring equest with null or empty id field: {} {}", jsonMethod, params);
            return;
        }

        LOG.trace("Request : {} {} {}", id, jsonMethod, params);

        final String method = jsonMethod.asText();
        switch (method) {
            case "echo":
                // Echo does not need any special processing. hence handling it internally.
                sendEmptyResponse(requestId);
                return;
            case "list_dbs":
                // send a null response for list_dbs
                sendEmptyResponse(requestId);
                return;
            default:
                if (!handleCallbackRequest(currentCallback, requestId, method, params)) {
                    LOG.error("No handler for Request : {} on {}", jsonRequest, nettyChannel);
                }
        }

    }

    private boolean handleCallbackRequest(final Callback callback, final String requestId, final String method,
            final JsonNode params) {
        if (callback == null) {
            // No callback registered: bail out
            return false;
        }

        switch (method) {
            case "update": {
                final UpdateNotification arg;
                try {
                    arg = objectMapper.convertValue(params, UpdateNotification.class);
                } catch (IllegalArgumentException e) {
                    return reportedMalformedParameters(requestId, e);
                }

                callback.update(nettyChannel, arg);
                return true;
            }
            case "locked": {
                final List<String> arg;
                try {
                    arg = objectMapper.convertValue(params, JT_LIST_STRING);
                } catch (IllegalArgumentException e) {
                    return reportedMalformedParameters(requestId, e);
                }

                callback.locked(nettyChannel, arg);
                return true;
            }
            case "stolen": {
                final List<String> arg;
                try {
                    arg = objectMapper.convertValue(params, JT_LIST_STRING);
                } catch (IllegalArgumentException e) {
                    return reportedMalformedParameters(requestId, e);
                }

                callback.stolen(nettyChannel, arg);
                return true;
            }
            default:
                return false;
        }
    }

    private boolean reportedMalformedParameters(final String requestId, final Exception cause) {
        LOG.debug("Request {} failed to map parameters", requestId, cause);
        sendErrorResponse(requestId, cause.getMessage());
        return true;
    }

    private void sendEmptyResponse(final String requestId) {
        sendErrorResponse(requestId, null);
    }

    private void sendErrorResponse(final String requestId, final String error) {
        JsonRpc10Response response = new JsonRpc10Response(requestId);
        response.setError(error);

        final String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            LOG.error("Exception while processing JSON response {}", response, e);
            return;
        }

        nettyChannel.writeAndFlush(jsonString);
    }

    private void handleResponse(final JsonNode response, final JsonNode result) {
        LOG.trace("Response : {}", response);
        final String requestId = response.get("id").asText();
        final CallContext returnCtxt = methodContext.remove(requestId);
        if (returnCtxt == null) {
            LOG.debug("Ignoring response for unknown request {}", requestId);
            return;
        }

        final JsonNode error = response.get("error");
        if (error != null && !error.isNull()) {
            LOG.error("Request {} failed with error {}", requestId, error);
        }

        final Object mappedResult = objectMapper.convertValue(result, returnCtxt.resultType);
        if (!returnCtxt.future.set(mappedResult)) {
            LOG.debug("Request {} did not accept result {}", requestId, mappedResult);
        }
    }

    private <T> ListenableFuture<T> sendRequest(final JsonRpc10Request request, final JavaType resultType) {
        final String requestString;
        try {
            requestString = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return Futures.immediateFailedFuture(e);
        }
        LOG.trace("getClient Request : {}", requestString);

        final SettableFuture<T> sf = SettableFuture.create();
        methodContext.put(request.getId(), new CallContext(resultType, sf));
        FUTURE_REAPER_SERVICE.schedule(() -> {
            CallContext cc = methodContext.remove(request.getId());
            if (cc != null) {
                if (cc.future.isDone() || cc.future.isCancelled()) {
                    return;
                }
                cc.future.cancel(false);
            }
        }, reaperInterval, TimeUnit.MILLISECONDS);

        nettyChannel.writeAndFlush(requestString);
        return sf;
    }

    private <T> ListenableFuture<T> sendRequest(final JavaType resultType, final String method) {
        return sendRequest(createRequest(method), resultType);
    }

    private <T> ListenableFuture<T> sendRequest(final JavaType resultType, final String method, final List params) {
        final JsonRpc10Request request = createRequest(method);
        request.setParams(params);
        return sendRequest(request, resultType);
    }

    private <T> ListenableFuture<T> sendRequest(final JavaType resultType, final String method, final Params params) {
        final JsonRpc10Request request = createRequest(method);
        request.setParams(params.params());
        return sendRequest(request, resultType);
    }

    private static JsonRpc10Request createRequest(final String method) {
        JsonRpc10Request request = new JsonRpc10Request(UUID.randomUUID().toString());
        request.setMethod(method);
        return request;
    }
}
