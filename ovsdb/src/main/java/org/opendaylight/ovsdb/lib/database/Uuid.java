package org.opendaylight.ovsdb.lib.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Uuid {
    @JsonProperty("uuid")
    public String[] uuid;
}
