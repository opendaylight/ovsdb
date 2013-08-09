package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Manager {
    Map<String, ManagerInfo> managerInfo = new HashMap<String, ManagerInfo>();

    @JsonAnySetter public void add(String key, ManagerInfo value) {
        managerInfo.put(key, value);
    }

    @JsonAnyGetter public Map<String,ManagerInfo> getProperties() {
        return managerInfo;
    }

    @Override
    public String toString() {
        return "Manager [managerInfo=" + managerInfo + "]";
    }
}