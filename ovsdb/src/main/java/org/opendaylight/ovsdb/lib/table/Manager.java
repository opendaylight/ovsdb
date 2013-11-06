package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Manager  extends Table<Manager> {

    public static final Name<Manager> NAME = new Name<Manager>("Manager") {};
    private String target;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    @JsonIgnore
    public Name<Manager> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Manager [target=" + target + "]";
    }
}
