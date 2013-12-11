/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Capability extends Table<Capability> {

    public static final Name<Capability> NAME = new Name<Capability>("Capability") {};
    private OvsDBMap<String, String> details;


    public OvsDBMap<String, String> getDetails() {
        return details;
    }

    public void setDetails(OvsDBMap<String, String> details) {
        this.details = details;
    }

    @Override
    @JsonIgnore
    public Name<Capability> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Capability [details=" + details + "]";
    }
}
