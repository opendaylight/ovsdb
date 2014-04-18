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

import java.util.List;

public class MutateRequest {
    public String op = "mutate";
    public String table;
    public List<Object> where;
    public List<Object> mutations;

    public MutateRequest(String table, List<Object> where, List<Object> mutations){
        this.table = table;
        this.where = where;
        this.mutations = mutations;
    }
}
