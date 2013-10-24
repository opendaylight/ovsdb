package org.opendaylight.ovsdb.lib.message;

public class MonitorSelect {

    boolean inital;
    boolean insert;
    boolean delete;
    boolean modify;

    public boolean isInital() {
        return inital;
    }

    public void setInital(boolean inital) {
        this.inital = inital;
    }

    public boolean isInsert() {
        return insert;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isModify() {
        return modify;
    }

    public void setModify(boolean modify) {
        this.modify = modify;
    }
}
