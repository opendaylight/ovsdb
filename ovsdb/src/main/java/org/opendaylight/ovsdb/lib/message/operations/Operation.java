package org.opendaylight.ovsdb.lib.message.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Operation {
    private String op;
    @JsonIgnore
    // Just a simple way to retain the result of a transact operation which the client can refer to.
    private OperationResult result;

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

    @Override
    public String toString() {
        return "Operation [op=" + op + ", result=" + result + "]";
    }
}
