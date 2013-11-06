package org.opendaylight.ovsdb.lib.message;

import java.util.ArrayList;

import org.opendaylight.ovsdb.lib.message.operations.OperationResult;

public class TransactResponse extends Response {
    ArrayList<OperationResult> result;

    public ArrayList<OperationResult> getResult() {
        return result;
    }

    public void setResult(ArrayList<OperationResult> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "TransactResponse [result=" + result + "]";
    }
}
