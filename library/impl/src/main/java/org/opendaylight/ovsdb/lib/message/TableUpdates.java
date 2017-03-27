/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.ovsdb.lib.schema.TableSchema;


public class TableUpdates extends Response {

    Map<String, TableUpdate> map = Maps.newHashMap();

    public TableUpdates() {
        this(Maps.newHashMap());
    }

    public TableUpdates(Map<String, TableUpdate> map) {
        this.map = map;
    }

    public <E extends TableSchema<E>> TableUpdate<E> getUpdate(TableSchema<E> table) {
        return this.map.get(table.getName());
    }

    public Map<String, TableUpdate> getUpdates() {
        return map;
    }

    @Override
    public String toString() {
        return "TableUpdates [map=" + map + ", error=" + error + ", details="
                + details + ", getError()=" + getError() + ", getDetails()="
                + getDetails() + ", getClass()=" + getClass() + ", hashCode()="
                + hashCode() + ", toString()=" + super.toString() + "]";
    }
}
