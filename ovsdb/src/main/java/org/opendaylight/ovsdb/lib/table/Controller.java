package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Controller  extends Table<Controller> {

    public static final Name<Controller> NAME = new Name<Controller>("Controller") {};
    private String target;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    @JsonIgnore
    public Name<Controller> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Controller [target=" + target + "]";
    }
}
