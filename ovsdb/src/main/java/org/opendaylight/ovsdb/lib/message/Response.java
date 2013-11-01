package org.opendaylight.ovsdb.lib.message;

public abstract class Response {
    Object error;
    Object details;

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

}
