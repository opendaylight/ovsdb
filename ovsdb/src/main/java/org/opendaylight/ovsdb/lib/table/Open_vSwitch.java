package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Open_vSwitch extends Table<Open_vSwitch> {

    public static final Name<Open_vSwitch> NAME = new Name<Open_vSwitch>("Open_vSwitch"){};

    private OvsDBSet<UUID> bridges;
    private Integer curr_cfg;
    private OvsDBSet<String> db_version;
    private OvsDBSet<UUID> manager_options;
    private OvsDBMap<String, String> status;
    private Integer next_cfg;
    private OvsDBSet<String> ovs_version;
    private OvsDBSet<UUID> ssl;
    private OvsDBSet<String> system_type;
    private OvsDBSet<String> system_version;
    private OvsDBMap<String, UUID> capabilities;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;
    private OvsDBMap<String, Integer> statistics;

    public Open_vSwitch() {
    }

    @Override
    public Name<Open_vSwitch> getTableName() {
        return NAME;
    }

    public OvsDBSet<UUID> getBridges() {
        return bridges;
    }

    public void setBridges(OvsDBSet<UUID> bridges) {
        this.bridges = bridges;
    }

    public Integer getCurr_cfg() {
        return curr_cfg;
    }

    public void setCurr_cfg(Integer curr_cfg) {
        this.curr_cfg = curr_cfg;
    }

    public OvsDBSet<String> getDb_version() {
        return db_version;
    }

    public void setDb_version(OvsDBSet<String> db_version) {
        this.db_version = db_version;
    }

    public OvsDBSet<UUID> getManager_options() {
        return manager_options;
    }

    public void setManager_options(OvsDBSet<UUID> manager_options) {
        this.manager_options = manager_options;
    }

    public OvsDBMap<String, String> getStatus() {
        return status;
    }

    public void setStatus(OvsDBMap<String, String> status) {
        this.status = status;
    }

    public Integer getNext_cfg() {
        return next_cfg;
    }

    public void setNext_cfg(Integer next_cfg) {
        this.next_cfg = next_cfg;
    }

    public OvsDBSet<String> getOvs_version() {
        return ovs_version;
    }

    public void setOvs_version(OvsDBSet<String> ovs_version) {
        this.ovs_version = ovs_version;
    }

    public OvsDBSet<UUID> getSsl() {
        return ssl;
    }

    public void setSsl(OvsDBSet<UUID> ssl) {
        this.ssl = ssl;
    }

    public OvsDBSet<String> getSystem_type() {
        return system_type;
    }

    public void setSystem_type(OvsDBSet<String> system_type) {
        this.system_type = system_type;
    }

    public OvsDBSet<String> getSystem_version() {
        return system_version;
    }

    public void setSystem_version(OvsDBSet<String> system_version) {
        this.system_version = system_version;
    }

    public OvsDBMap<String, UUID> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(OvsDBMap<String, UUID> capabilities) {
        this.capabilities = capabilities;
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

    public OvsDBMap<String, Integer> getStatistics() {
        return statistics;
    }

    public void setStatistics(OvsDBMap<String, Integer> statistics) {
        this.statistics = statistics;
    }

    @Override
    public String toString() {
        return "Open_vSwitch [bridges=" + bridges + ", curr_cfg=" + curr_cfg
                + ", db_version=" + db_version + ", manager_options="
                + manager_options + ", status=" + status + ", next_cfg="
                + next_cfg + ", ovs_version=" + ovs_version + ", ssl=" + ssl
                + ", system_type=" + system_type + ", system_version="
                + system_version + ", capabilities=" + capabilities
                + ", other_config=" + other_config + ", external_ids="
                + external_ids + ", statistics=" + statistics + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Open_vSwitch>{ controller, fail_mode, name, ports}
}
