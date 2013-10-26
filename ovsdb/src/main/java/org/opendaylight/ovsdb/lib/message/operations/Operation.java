package org.opendaylight.ovsdb.lib.message.operations;

public abstract class Operation {
    private String op;

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }
}
