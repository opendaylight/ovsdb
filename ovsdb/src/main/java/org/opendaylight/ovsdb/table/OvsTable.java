package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;


public class OvsTable {
    Map<String, OvsTableInfo> ovstableInfo = new HashMap<String, OvsTableInfo>();

    @JsonAnySetter public void add(String key, OvsTableInfo value) {
        ovstableInfo.put(key, value);
    }

    @JsonAnyGetter public Map<String,OvsTableInfo> getProperties() {
        return ovstableInfo;
    }

    @Override
    public String toString() {
        return "OvsTable [ovstableInfo=" + ovstableInfo + "]";
    }
}