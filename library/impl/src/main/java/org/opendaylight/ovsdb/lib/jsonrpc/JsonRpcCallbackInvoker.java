/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC.Callback;

final class JsonRpcCallbackInvoker {
    // Static pointer to interface methods, which are well-known
    private static final ImmutableMap<String, JsonRpcCallbackInvoker> INVOKERS;

    static {
        final ImmutableMap.Builder<String, JsonRpcCallbackInvoker> builder = ImmutableMap.builder();
        for (Method method : Callback.class.getDeclaredMethods()) {
            builder.put(method.getName(), new JsonRpcCallbackInvoker(method));
        }
        INVOKERS = builder.build();
    }

    private final Method method;
    private final Class<?> secondParameter;

    private JsonRpcCallbackInvoker(final Method method) {
        this.method = requireNonNull(method);
        secondParameter = method.getParameterTypes()[1];
    }

    static @Nullable JsonRpcCallbackInvoker forMethod(final String methodName) {
        return INVOKERS.get(requireNonNull(methodName));
    }

    void invokeMethod(final ObjectMapper mapper, final Callback receiver, final Object context, final JsonNode params)
            throws IllegalAccessException, InvocationTargetException {
        method.invoke(receiver, context, mapper.convertValue(params, secondParameter));
    }
}
