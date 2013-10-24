package org.opendaylight.ovsdb.table;

import org.opendaylight.ovsdb.datatype.OvsDBMap;
import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.datatype.UUID;
import org.opendaylight.ovsdb.table.internal.Table;

public class Mirror  extends Table<Mirror> {

    public static final Name<Mirror> NAME = new Name<Mirror>("Mirror") {};
    private String name;
    private OvsDBSet<UUID> select_src_port;
    private OvsDBSet<UUID> select_dst_port;
    private OvsDBSet<Integer> select_vlan;
    private OvsDBSet<UUID> output_port;
    private OvsDBSet<Integer> output_vlan;
    private OvsDBMap<String, Integer> statistics;
    private OvsDBMap<String, String> external_ids;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OvsDBSet<UUID> getSelect_src_port() {
        return select_src_port;
    }

    public void setSelect_src_port(OvsDBSet<UUID> select_src_port) {
        this.select_src_port = select_src_port;
    }

    public OvsDBSet<UUID> getSelect_dst_port() {
        return select_dst_port;
    }

    public void setSelect_dst_port(OvsDBSet<UUID> select_dst_port) {
        this.select_dst_port = select_dst_port;
    }

    public OvsDBSet<Integer> getSelect_vlan() {
        return select_vlan;
    }

    public void setSelect_vlan(OvsDBSet<Integer> select_vlan) {
        this.select_vlan = select_vlan;
    }

    public OvsDBSet<UUID> getOutput_port() {
        return output_port;
    }

    public void setOutput_port(OvsDBSet<UUID> output_port) {
        this.output_port = output_port;
    }

    public OvsDBSet<Integer> getOutput_vlan() {
        return output_vlan;
    }

    public void setOutput_vlan(OvsDBSet<Integer> output_vlan) {
        this.output_vlan = output_vlan;
    }

    public OvsDBMap<String, Integer> getStatistics() {
        return statistics;
    }

    public void setStatistics(OvsDBMap<String, Integer> statistics) {
        this.statistics = statistics;
    }

    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }

    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }

    @Override
    public Name<Mirror> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Mirror [name=" + name + ", select_src_port=" + select_src_port
                + ", select_dst_port=" + select_dst_port + ", select_vlan="
                + select_vlan + ", output_port=" + output_port
                + ", output_vlan=" + output_vlan + ", statistics=" + statistics
                + ", external_ids=" + external_ids + "]";
    }
}
