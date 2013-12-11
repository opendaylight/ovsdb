/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.northbound;

import java.util.Map;

import org.opendaylight.ovsdb.lib.table.internal.Table;

public class OVSDBRows {
    Map<String, Table<?>> rows;

    public OVSDBRows(Map<String, Table<?>> rows) {
        super();
        this.rows = rows;
    }

    public Map<String, Table<?>> getRows() {
        return rows;
    }

    public void setRows(Map<String, Table<?>> rows) {
        this.rows = rows;
    }
}
