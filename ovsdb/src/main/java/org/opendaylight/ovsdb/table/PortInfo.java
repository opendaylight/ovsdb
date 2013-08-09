package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PortInfo {
    @JsonProperty("new")
    New unique;

    @Override
    public String toString() {
        return "PortInfo [unique=" + unique + "]";
    }
}