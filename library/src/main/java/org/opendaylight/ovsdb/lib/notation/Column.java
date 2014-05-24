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

package org.opendaylight.ovsdb.lib.notation;


import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;


public class Column<E extends TableSchema<E>, D> {

    private ColumnSchema<E, D> schema;
    private D data;

    public Column(ColumnSchema<E, D> schema, D d) {
        this.schema = schema;
        this.data = d;
    }

    public <E extends TableSchema<E>, T> T getData(ColumnSchema<E, T> schema) {
        return schema.validate(data);
    }

    public Object getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }

    public ColumnSchema<E, D> getSchema() {
        return schema;
    }

    public void setSchema(ColumnSchema<E, D> schema) {
        this.schema = schema;
    }
}
