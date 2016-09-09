/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public abstract class Operation {

    @JsonIgnore
    private TableSchema tableSchema;

    private String op;

    @JsonIgnore
    //todo(Ashwin): remove this
    // Just a simple way to retain the result of a transact operation which the client can refer to.
    private OperationResult result;


    protected Operation() {
    }

    protected Operation(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
    }

    public Operation(TableSchema schema, String operation) {
        this.tableSchema = schema;
        this.op = operation;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public TableSchema getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
    }

    @JsonProperty
    public String getTable() {
        return (tableSchema == null) ? null : tableSchema.getName();
    }

    @Override
    public String toString() {
        return "Operation [op=" + op + ", result=" + result + "]";
    }

}
