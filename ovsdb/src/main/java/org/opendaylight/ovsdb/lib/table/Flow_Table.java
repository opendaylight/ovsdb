package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Per ovs-vswitchd.conf.db 2.0.90
 *
 * Summary of the Flow_Table fields:
 * name optional    string
 * flow_limit       integer, at least 0 (optional)
 * overflow_policy  optional string, either refuse or evict
 * groups           set of strings
 */

public class Flow_Table extends Table<Flow_Table> {

    public static final Name<Flow_Table> NAME = new Name<Flow_Table>("Flow_Table") {};

    private OvsDBSet<String> name;
    private OvsDBSet<Integer> flow_limit;
    private OvsDBSet<String> overflow_policy;
    private OvsDBSet<String> groups;

    public Flow_Table() {
    }

    public OvsDBSet<Integer> getFlow_limit() {
        return flow_limit;
    }

    public void setFlow_limit(OvsDBSet<Integer> flow_limit) {
        this.flow_limit = flow_limit;
    }

    public OvsDBSet<String> getOverflow_policy() {
        return overflow_policy;
    }

    public void setOverflow_policy(OvsDBSet<String> overflow_policy) {
        this.overflow_policy = overflow_policy;
    }

    public OvsDBSet<String> getGroups() {
        return groups;
    }

    public void setGroups(OvsDBSet<String> groups) {
        this.groups = groups;
    }

    @Override
    @JsonIgnore
    public Name<Flow_Table> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Flow_Table [" +
                "name=" + name +
                ", flow_limit=" + flow_limit +
                ", overflow_policy=" + overflow_policy +
                ", groups=" + groups +
                "]";
    }


    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Flow_Table> {
        name,
        flow_limit,
        overflow_policy,
        groups,

    }
}