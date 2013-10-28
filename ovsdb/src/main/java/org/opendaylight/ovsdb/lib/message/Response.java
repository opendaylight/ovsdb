package org.opendaylight.ovsdb.lib.message;

public abstract class Response {
    Object error;

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }
}
