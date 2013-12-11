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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;

import java.util.List;

import org.opendaylight.ovsdb.lib.table.internal.Column;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorRequest<E> {

    //@JsonSerialize(contentAs = ToStringSerializer.class)
    List<Column<E>> columns;

    MonitorSelect select;

    public List<? extends Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column<E>> columns) {
        this.columns = columns;
    }


    public MonitorSelect getSelect() {
        return select;
    }

    public void setSelect(MonitorSelect select) {
        this.select = select;
    }

    public MonitorRequest<E> column(Column<E> column) {
        if (null == columns) {
            columns = Lists.newArrayList();
        }
        columns.add(column);
        return this;
    }
}
