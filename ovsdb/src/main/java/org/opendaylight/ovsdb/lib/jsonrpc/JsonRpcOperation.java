package org.opendaylight.ovsdb.lib.jsonrpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface JsonRpcOperation {
    String DEFAULT_VALUE = "METHOD_NAME";
    String value() default DEFAULT_VALUE;
}
