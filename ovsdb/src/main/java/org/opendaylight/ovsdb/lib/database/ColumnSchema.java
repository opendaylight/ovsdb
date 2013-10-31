package org.opendaylight.ovsdb.lib.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ColumnSchema {
    @JsonProperty("type")
    private OvsdbType type;
    @JsonProperty("ephemeral")
    private Boolean ephemeral;
    @JsonProperty("mutable")
    private Boolean mutable;
    public OvsdbType getType() {
        return type;
    }
    public Boolean getEphemeral() {
        return ephemeral;
    }
    public Boolean getMutable() {
        return mutable;
    }
    @Override
    public String toString() {
        return "ColumnSchema [type=" + type + ", ephemeral=" + ephemeral
                + ", mutable=" + mutable + "]";
    }
}
