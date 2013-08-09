package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OvsTableInfo {
    @JsonProperty("new")
    New unique;

    @Override
    public String toString() {
        return "OvsTableInfo [unique=" + unique + "]";
    }
}