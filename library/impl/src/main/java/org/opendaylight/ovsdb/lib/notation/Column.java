/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;


import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class Column<E extends TableSchema<E>, D> {
    @JsonIgnore
    private ColumnSchema<E, D> schema;
    private D data;

    public Column(ColumnSchema<E, D> schema, D data) {
        this.schema = schema;
        this.data = data;
    }

    public <E extends TableSchema<E>, T> T getData(ColumnSchema<E, T> schema) {
        return schema.validate(data);
    }

    public D getData() {
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

    @Override
    public String toString() {
        return "[" + schema.getName() + "=" + data + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((schema == null) ? 0 : schema.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Column other = (Column) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (schema == null) {
            if (other.schema != null) {
                return false;
            }
        } else if (!schema.equals(other.schema)) {
            return false;
        }
        return true;
    }
}
