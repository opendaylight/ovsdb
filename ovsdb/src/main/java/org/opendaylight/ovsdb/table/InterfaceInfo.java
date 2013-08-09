package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InterfaceInfo {
    @JsonProperty("new")
    New unique;

    @Override
    public String toString() {
        return "InterfaceInfo [unique=" + unique + "]";
    }
}