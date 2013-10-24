package org.opendaylight.ovsdb.table;

import org.opendaylight.ovsdb.table.internal.Table;

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
    public Name<Controller> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Controller [target=" + target + "]";
    }
}
