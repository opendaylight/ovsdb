/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.schema.typed;

import org.opendaylight.ovsdb.lib.notation.Version;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TypedTable {
    String name();
    String database();
    String fromVersion() default Version.NULL_VERSION_STRING;
    String untilVersion() default Version.NULL_VERSION_STRING;
}
