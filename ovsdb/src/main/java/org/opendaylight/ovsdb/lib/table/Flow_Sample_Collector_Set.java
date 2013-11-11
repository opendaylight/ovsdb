package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Per ovs-vswitchd.conf.db 2.0.90
 *
 * A summary of the Flow_Sample_Collector_Set fields:
 * bridge           Bridge
 * ipﬁx             IPFIX (optional)
 * external_ids     map of string-string pairs
 */

public class Flow_Sample_Collector_Set extends Table<Flow_Sample_Collector_Set> {

    public static final Name<Flow_Sample_Collector_Set> NAME = new Name<Flow_Sample_Collector_Set>("Flow_Sample_Collector_Set") {};

    private OvsDBSet<Integer> id;
    private OvsDBSet<UUID> bridge;
    private OvsDBSet<UUID> IPFIX;
    private OvsDBMap<String, String> external_ids;

    public OvsDBSet<Integer> getId() {
        return id;
    }

    public void setId(OvsDBSet<Integer> id) {
        this.id = id;
    }

    public OvsDBSet<UUID> getBridge() {
        return bridge;
    }

    public void setBridge(OvsDBSet<UUID> bridge) {
        this.bridge = bridge;
    }

    public OvsDBSet<UUID> getIPFIX() {
        return IPFIX;
    }

    public void setIPFIX(OvsDBSet<UUID> IPFIX) {
        this.IPFIX = IPFIX;
    }

    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }

    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }

    @Override
    @JsonIgnore
    public Name<Flow_Sample_Collector_Set> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Flow_Sample_Collector_Set [" +
                "id=" + id +
                ", bridge=" + bridge +
                ", IPFIX=" + IPFIX +
                ", external_ids=" + external_ids +
                "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Flow_Sample_Collector_Set> {
        id,
        bridge,
        external_ids,
    }
}
