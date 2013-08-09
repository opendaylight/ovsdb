package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Bridge {
    Map<String, BridgeInfo> bridgeInfo = new HashMap<String, BridgeInfo>();

    @JsonAnySetter public void add(String key, BridgeInfo value) {
        bridgeInfo.put(key, value);
    }

    @JsonAnyGetter public Map<String,BridgeInfo> getProperties() {
        return bridgeInfo;
    }

    @Override
    public String toString() {
        return "Bridge [bridgeInfo=" + bridgeInfo + "]";
    }
}