package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NetFlow  extends Table<NetFlow> {

    public static final Name<NetFlow> NAME = new Name<NetFlow>("NetFlow") {};
    private OvsDBSet<String> targets;
    private Integer active_timeout;

    public OvsDBSet<String> getTargets() {
        return targets;
    }

    public Integer getActiveTimeout() {
        return active_timeout;
    }

    public void setTargets(OvsDBSet<String> targets) {
        this.targets = targets;
    }

    @Override
    @JsonIgnore
    public Name<NetFlow> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "NetFlow [targets=" + targets + "]";
    }
}
