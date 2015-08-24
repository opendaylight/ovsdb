/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

public enum Function {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">="),
    GREATER_THAN_OR_EQUALS(">="),
    INCLUDES("includes"),
    EXCLUDES("excludes");

    private Function(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
