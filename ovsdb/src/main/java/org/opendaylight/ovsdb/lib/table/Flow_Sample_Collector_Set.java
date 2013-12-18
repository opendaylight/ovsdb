package org.opendaylight.ovsdb.lib.table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.table.internal.Table;

/**
 * Per ovs-vswitchd.conf.db 2.0.90
 *
 * A summary of the Flow_Sample_Collector_Set fields:
 * bridge           Bridge
 * ipﬁx             ipfix (optional)
 * external_ids     map of string-string pairs
 */

public class Flow_Sample_Collector_Set extends Table<Flow_Sample_Collector_Set> {

    public static final Name<Flow_Sample_Collector_Set> NAME = new Name<Flow_Sample_Collector_Set>("Flow_Sample_Collector_Set") {};

    private Integer id;
    private Bridge bridge;
    private IPFIX ipfix;
    private OvsDBMap<String, String> external_ids;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }

    public IPFIX getIpfix() {
        return ipfix;
    }

    public void setIpfix(IPFIX ipfix) {
        this.ipfix = ipfix;
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
                ", ipfix=" + ipfix +
                ", external_ids=" + external_ids +
                "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Flow_Sample_Collector_Set> {
        id,
        bridge,
        external_ids,
    }
}
