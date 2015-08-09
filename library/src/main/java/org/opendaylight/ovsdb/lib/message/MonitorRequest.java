/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import java.util.Set;

import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;

/**
 * @author Ashwin Raveendran
 * @author Madhu Venugopal
 * @param <E> monitor
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorRequest<E extends TableSchema<E>> {
    @JsonIgnore String tableName;
    Set<String> columns;
    MonitorSelect select;

    public MonitorRequest() {
    }

    public MonitorRequest(String tableName, Set<String> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public MonitorRequest(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public MonitorSelect getSelect() {
        return select;
    }

    public void setSelect(MonitorSelect select) {
        this.select = select;
    }

    public Set<String> getColumns() {
        return columns;
    }

    public void setColumns(Set<String> columns) {
        this.columns = columns;
    }

    public void addColumn(String column) {
        if (columns == null) {
            columns = Sets.<String>newHashSet();
        }
        columns.add(column);
    }
}
