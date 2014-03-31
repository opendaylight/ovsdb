/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
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

    public TableSchema getTable(String tableName) {
       return tables.get(tableName);
    }

    @Override
    public String toString() {
        return "DatabaseSchema [name=" + name + ", version=" + version
                + ", cksum=" + cksum + ", tables=" + tables + "]";
    }
}
