package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManagerInfo {
    @JsonProperty("new")
    New unique;

    @Override
    public String toString() {
        return "ManagerInfo [unique=" + unique + "]";
    }
}