package org.opendaylight.ovsdb.lib.table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;

/**
 * Per ovs-vswitchd.conf.db 2.0.90
 *
 * Summary of the IPFIX fields:
 * targets               set of 1 or more strings
 * sampling              integer, in range 1 to 4,294,967,295 (optional)
 * obs_domain_id         integer, in range 0 to 4,294,967,295 (optional)
 * obs_point_id          integer, in range 0 to 4,294,967,295 (optional)
 * cache_active_timeout  integer, in range 0 to 4,200 (optional)
 * cache_max_ﬂows        integer, in range 0 to 4,294,967,295 (optional)
 */

public class IPFIX extends Table<IPFIX> {

    public static final Name<IPFIX> NAME = new Name<IPFIX>("IPFIX") {};

    private OvsDBSet<String> targets;
    private OvsDBSet<Integer> sampling;
    private Integer obs_domain_id;
    private Integer obs_point_id;
    private OvsDBSet<Integer> cache_active_timeout;
    private OvsDBSet<Integer> cache_max_ﬂows;
    private OvsDBMap<String, String> external_ids;

    public IPFIX() {
    }

    public OvsDBSet<String> getTargets() {
        return targets;
    }

    public void setTargets(OvsDBSet<String> targets) {
        this.targets = targets;
    }

    public OvsDBSet<Integer> getSampling() {
        return sampling;
    }

    public void setSampling(OvsDBSet<Integer> sampling) {
        this.sampling = sampling;
    }

    public Integer getObs_domain_id() {
        return obs_domain_id;
    }

    public void setObs_domain_id(Integer obs_domain_id) {
        this.obs_domain_id = obs_domain_id;
    }

    public Integer getObs_point_id() {
        return obs_point_id;
    }

    public void setObs_point_id(Integer obs_point_id) {
        this.obs_point_id = obs_point_id;
    }

    public OvsDBSet<Integer> getCache_active_timeout() {
        return cache_active_timeout;
    }

    public void setCache_active_timeout(OvsDBSet<Integer> cache_active_timeout) {
        this.cache_active_timeout = cache_active_timeout;
    }

    public OvsDBSet<Integer> getCache_max_ﬂows() {
        return cache_max_ﬂows;
    }

    public void setCache_max_ﬂows(OvsDBSet<Integer> cache_max_ﬂows) {
        this.cache_max_ﬂows = cache_max_ﬂows;
    }

    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }

    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }

    @Override
    @JsonIgnore
    public Name<IPFIX> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "IPFIX [" +
                "targets=" + targets +
                ", sampling=" + sampling +
                ", obs_domain_id=" + obs_domain_id +
                ", obs_point_id=" + obs_point_id +
                ", cache_active_timeout=" + cache_active_timeout +
                ", cache_max_ﬂows=" + cache_max_ﬂows +
                ", external_ids=" + external_ids +
                "]";
    }
}
