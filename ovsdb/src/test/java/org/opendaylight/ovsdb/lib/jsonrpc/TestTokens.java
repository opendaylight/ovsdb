/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Aswin Raveendran
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTokens {
    protected static final Logger logger = LoggerFactory.getLogger(TestTokens.class);
    public ListenableFuture<String> getString() {
        return null;
    }


    public static void main(String[] args) throws NoSuchMethodException {
        Method getString = TestTokens.class.getMethod("getString");
        Invokable<?, Object> from = Invokable.from(getString);
        //TypeToken<?> get = from.getReturnType().resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
        TypeToken<?> get = from.getReturnType().resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
        logger.info("",get.getRawType());
        TypeToken<?> get1 = TypeToken.of(getString.getGenericReturnType()).resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
        logger.info("get1 = ",get1);
    }

}
