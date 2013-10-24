package org.opendaylight.ovsdb.table;

import org.opendaylight.ovsdb.datatype.OvsDBMap;
import org.opendaylight.ovsdb.table.internal.Table;

public class Capability extends Table<Capability> {

    public static final Name<Capability> NAME = new Name<Capability>("Capability") {};
    private OvsDBMap<String, String> details;


    public OvsDBMap<String, String> getDetails() {
        return details;
    }

    public void setDetails(OvsDBMap<String, String> details) {
        this.details = details;
    }

    @Override
    public Name<Capability> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Capability [details=" + details + "]";
    }
}
