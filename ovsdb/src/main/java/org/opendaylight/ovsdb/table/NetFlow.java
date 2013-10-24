package org.opendaylight.ovsdb.table;

import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.table.internal.Table;

public class NetFlow  extends Table<NetFlow> {

    public static final Name<NetFlow> NAME = new Name<NetFlow>("NetFlow") {};
    private OvsDBSet<String> targets;

    public OvsDBSet<String> getTargets() {
        return targets;
    }

    public void setTargets(OvsDBSet<String> targets) {
        this.targets = targets;
    }

    @Override
    public Name<NetFlow> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "NetFlow [targets=" + targets + "]";
    }
}
