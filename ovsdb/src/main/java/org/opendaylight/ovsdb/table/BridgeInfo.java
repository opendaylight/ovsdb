package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BridgeInfo {
    @JsonProperty("new")
    New unique;

    @Override
    public String toString() {
        return "BridgeInfo [unique=" + unique + "]";
    }
}