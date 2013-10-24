package org.opendaylight.ovsdb.internal.jsonrpc;

import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class TestTokens {

    public ListenableFuture<String> getString() {
        return null;
    }


    public static void main(String[] args) throws NoSuchMethodException {
        Method getString = TestTokens.class.getMethod("getString");
        Invokable<?, Object> from = Invokable.from(getString);
        //TypeToken<?> get = from.getReturnType().resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
        TypeToken<?> get = from.getReturnType().resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
        System.out.println(get.getRawType());


        TypeToken<?> get1 = TypeToken.of(getString.getGenericReturnType()).resolveType(ListenableFuture.class.getMethod("get").getGenericReturnType());
        System.out.println("get1 = " + get1);
    }

}
