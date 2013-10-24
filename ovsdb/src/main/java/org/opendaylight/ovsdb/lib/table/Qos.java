package org.opendaylight.ovsdb.lib.table;

import java.math.BigInteger;

import org.opendaylight.ovsdb.lib.datatype.OvsDBMap;
import org.opendaylight.ovsdb.lib.datatype.OvsDBSet;
import org.opendaylight.ovsdb.lib.datatype.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Qos extends Table<Qos> {

    public static final Name<Qos> NAME = new Name<Qos>("QoS") {};

    private OvsDBMap<Integer, UUID> queues;
    private Integer type;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public Qos() {
    }

    public OvsDBMap<Integer, UUID> getQueues() {
        return queues;
    }

    public void setQueues(OvsDBMap<Integer, UUID> queues) {
        this.queues = queues;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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
    public Name<Qos> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Qos [queues=" + queues + ", type=" + type + ", other_config="
                + other_config + ", external_ids=" + external_ids + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Qos> {
        queues,
        type,
        other_config,
        external_ids}
}
