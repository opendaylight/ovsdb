/*
 * Copyright (C) 2013 of individual owners listed as Authors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.plugin;

public enum Encapsulation {

    VXLAN("vxlan"), GRE("gre"), CAPWAP("capwap");

    private final String value;

    private Encapsulation(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
