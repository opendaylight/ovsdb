/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */
package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Maps;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.Map;


public class TableUpdates extends Response {

    Map<String, TableUpdate> map = Maps.newHashMap();

    public TableUpdates() {
        this(Maps.<String, TableUpdate>newHashMap());
    }

    public TableUpdates(Map<String, TableUpdate> map) {
        this.map = map;
    }

    public <E extends TableSchema<E>> TableUpdate<E> getUpdate(TableSchema<E> table) {
        return this.map.get(table.getName());
    }
}
