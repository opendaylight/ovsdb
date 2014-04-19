/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorRequest<E extends TableSchema<E>> {
    @JsonIgnore String tableName;
    Set<String> columns = Sets.newHashSet();
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

}
