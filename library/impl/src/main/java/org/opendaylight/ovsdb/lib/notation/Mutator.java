/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

public enum Mutator {
    SUM("+="),
    DIFFERENCE("-="),
    PRODUCT("*="),
    QUOTIENT("/="),
    REMAINDER("%="),
    INSERT("insert"),
    DELETE("delete");

    private Mutator(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
