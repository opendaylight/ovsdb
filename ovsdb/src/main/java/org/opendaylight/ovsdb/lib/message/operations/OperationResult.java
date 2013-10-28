package org.opendaylight.ovsdb.lib.message.operations;

import java.util.ArrayList;
import org.opendaylight.ovsdb.lib.notation.UUID;

// Section 5.2 of ovsdb draft covers the various response structures for
// each of the Operations covered by Transaction (Insert, Update, Delete, Mutate, etc...)
// It is better to have the OperationResult as an abstract parent class with individual
// concrete child classes for each of the operation response.
// But this needs proper response handling
// https://trello.com/c/mfTTS86k/28-generic-response-error-handling-especially-for-transact
// As a temporary measure, adding all the expected responses under the same response.

public class OperationResult {
    //public abstract boolean isSuccess();
    private int count;
    private UUID uuid;
    private ArrayList<Object> rows;
    private String error;

    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public ArrayList<Object> getRows() {
        return rows;
    }
    public void setRows(ArrayList<Object> rows) {
        this.rows = rows;
    }
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }
    @Override
    public String toString() {
        return "OperationResult [count=" + count + ", uuid=" + uuid + ", rows="
                + rows + ", error=" + error + "]";
    }
}
