package org.opendaylight.ovsdb.table;

import java.math.BigInteger;

import org.opendaylight.ovsdb.datatype.OvsDBMap;
import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.datatype.UUID;
import org.opendaylight.ovsdb.table.internal.Table;

public class Queue extends Table<Queue> {

    public static final Name<Queue> NAME = new Name<Queue>("Queue") {};

    private OvsDBSet<Integer> dscp;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public Queue() {
    }

    public OvsDBSet<Integer> getDscp() {
        return dscp;
    }

    public void setDscp(OvsDBSet<Integer> dscp) {
        this.dscp = dscp;
    }

    public OvsDBMap<String, String> getOther_config() {
        return other_config;
    }

    public void setOther_config(OvsDBMap<String, String> other_config) {
        this.other_config = other_config;
    }

    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }

    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }

    @Override
    public Name<Queue> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Queue [dscp=" + dscp + ", other_config=" + other_config
                + ", external_ids=" + external_ids + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.table.internal.Column<Queue> {
        dscp,
        other_config,
        external_ids}
}
