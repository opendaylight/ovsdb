/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * MonitorRequest.
 *
 * @author Ashwin Raveendran
 * @author Madhu Venugopal
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorRequest {

    private final @JsonIgnore String tableName;
    private final Set<String> columns;
    private final MonitorSelect select;

    MonitorRequest(String tableName, Set<String> columns, MonitorSelect select) {
        this.tableName = tableName;
        this.columns = ImmutableSet.copyOf(columns);
        this.select = select;
    }

    public String getTableName() {
        return tableName;
    }

    public MonitorSelect getSelect() {
        return select;
    }

    public Set<String> getColumns() {
        return columns;
    }

}
