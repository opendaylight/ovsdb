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

public class UpdateRequest {
    public String op;
    public String table;
    public ArrayList<Object> where;
    public Map<String, Object> row;

    public UpdateRequest(String op, String table, ArrayList<Object> where, Map<String, Object> row){
        this.op = op;
        this.table = table;
        this.where = where;
        this.row = row;
    }
}
