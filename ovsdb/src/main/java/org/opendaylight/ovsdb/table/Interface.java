package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Interface {
    Map<String, InterfaceInfo> interfaceInfo = new HashMap<String, InterfaceInfo>();

    @JsonAnySetter public void add(String key, InterfaceInfo value) {
        interfaceInfo.put(key, value);
    }

    @JsonAnyGetter public Map<String,InterfaceInfo> getProperties() {
        return interfaceInfo;
    }

    @Override
    public String toString() {
        return "Interface [interfaceInfo=" + interfaceInfo + "]";
    }
}