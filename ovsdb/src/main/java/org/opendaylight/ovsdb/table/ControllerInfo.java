package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ControllerInfo {
    @JsonProperty("new")
    New unique;

    @Override
    public String toString() {
        return "ControllerInfo [unique="
         + unique + "]";
    }
}