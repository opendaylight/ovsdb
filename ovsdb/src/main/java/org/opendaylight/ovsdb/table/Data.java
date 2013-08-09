package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
/*
 * Adding as a root reference,table
 * POJOs will likely be called individually
*/
public class Data {
    @JsonProperty("Port")
    Port port;

    @JsonProperty("Bridge")
    Bridge Bridge;

    @JsonProperty("Interface")
    Interface Interface;

    @JsonProperty("OvsTable")
    OvsTable OvsTable;

    @JsonProperty("Controller")
    Controller Controller;

    @JsonProperty("Manager")
    Manager Manager;

    @Override
    public String toString() {
        return "Data{" +
                "port=" + port +
                ", Bridge=" + Bridge +
                ", Interface=" + Interface +
                ", OvsTable=" + OvsTable +
                ", Controller=" + Controller +
                ", Manager=" + Manager +
                '}';
    }
}