package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Interface extends Table<Interface> {

    public static Name<Interface> NAME = new Name<Interface>("Interface") {};

    private String name;
    private OvsDBMap<String, String> options;
    private String type;
    private OvsDBSet<Integer> ofport;
    private OvsDBSet<String> mac;
    private OvsDBMap<String, Integer> statistics;
    private OvsDBMap<String, String> status;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OvsDBMap<String, String> getOptions() {
        return options;
    }

    public void setOptions(OvsDBMap<String, String> options) {
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OvsDBSet<Integer> getOfport() {
        return ofport;
    }

    public void setOfport(OvsDBSet<Integer> ofport) {
        this.ofport = ofport;
    }

    public OvsDBSet<String> getMac() {
        return mac;
    }

    public void setMac(OvsDBSet<String> mac) {
        this.mac = mac;
    }

    public OvsDBMap<String, Integer> getStatistics() {
        return statistics;
    }

    public void setStatistics(OvsDBMap<String, Integer> statistics) {
        this.statistics = statistics;
    }

    public OvsDBMap<String, String> getStatus() {
        return status;
    }

    public void setStatus(OvsDBMap<String, String> status) {
        this.status = status;
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
    public Name<Interface> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Interface [name=" + name + ", options=" + options + ", type="
                + type + ", ofport=" + ofport + ", mac=" + mac
                + ", statistics=" + statistics + ", status=" + status
                + ", other_config=" + other_config + ", external_ids="
                + external_ids + "]";
    }
}
