package org.opendaylight.ovsdb.table;

import org.opendaylight.ovsdb.datatype.OvsDBMap;
import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.table.internal.Table;

public class SFlow  extends Table<SFlow> {

    public static final Name<SFlow> NAME = new Name<SFlow>("sFlow") {};
    private OvsDBSet<String> agent;
    private OvsDBSet<String> targets;
    private OvsDBMap<String, String> external_ids;
    private OvsDBSet<Integer> header;
    private OvsDBSet<Integer> polling;
    private OvsDBSet<Integer> sampling;

    public OvsDBSet<String> getTargets() {
        return targets;
    }

    public void setTargets(OvsDBSet<String> targets) {
        this.targets = targets;
    }

    @Override
    public Name<SFlow> getTableName() {
        return NAME;
    }

    public OvsDBSet<String> getAgent() {
        return agent;
    }

    public void setAgent(OvsDBSet<String> agent) {
        this.agent = agent;
    }

    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }

    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }

    public OvsDBSet<Integer> getHeader() {
        return header;
    }

    public void setHeader(OvsDBSet<Integer> header) {
        this.header = header;
    }

    public OvsDBSet<Integer> getPolling() {
        return polling;
    }

    public void setPolling(OvsDBSet<Integer> polling) {
        this.polling = polling;
    }

    public OvsDBSet<Integer> getSampling() {
        return sampling;
    }

    public void setSampling(OvsDBSet<Integer> sampling) {
        this.sampling = sampling;
    }

    @Override
    public String toString() {
        return "SFlow [agent=" + agent + ", targets=" + targets
                + ", external_ids=" + external_ids + ", header=" + header
                + ", polling=" + polling + ", sampling=" + sampling + "]";
    }
}
