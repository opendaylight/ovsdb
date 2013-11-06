package org.opendaylight.ovsdb.lib.database;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseSchema {
    @JsonProperty("name")
    private String name;
    @JsonProperty("version")
    private String version;
    @JsonProperty("cksum")
    private String cksum;
    @JsonProperty("tables")
    private Map<String, TableSchema> tables;
    public String getName() {
        return name;
    }
    public String getVersion() {
        return version;
    }
    public String getCksum() {
        return cksum;
    }
    public Map<String, TableSchema> getTables() {
        return tables;
    }
    @Override
    public String toString() {
        return "DatabaseSchema [name=" + name + ", version=" + version
                + ", cksum=" + cksum + ", tables=" + tables + "]";
    }
}
