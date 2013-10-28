package org.opendaylight.ovsdb.lib.message.operations;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonIgnore
    private UUID uuid;
    private ArrayList<Object> rows;
    private String error;
    private String details;

    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    @JsonProperty("uuid")
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(List<String> uuidList) {
        this.uuid = new UUID(uuidList.get(1));
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
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "OperationResult [count=" + count + ", uuid=" + uuid + ", rows="
                + rows + ", error=" + error + "]";
    }
}
