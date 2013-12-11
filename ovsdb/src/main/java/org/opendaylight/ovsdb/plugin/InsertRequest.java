/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.opendaylight.ovsdb.lib.database.OvsdbType;

public class InsertRequest {
    public String op;
    public String table;
    @JsonProperty("uuid-name")
    public Object uuidName;
    public Map<String, Object> row;

    public InsertRequest(String op, String table, String uuidName, Map<String, Object> row){
        this.op = op;
        this.table = table;
        this.uuidName = uuidName;
        this.row = row;
    }
}
