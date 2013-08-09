package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bridge {

    Map<String, BridgeInfo> bridgeInfo = new HashMap<String, BridgeInfo>();

    private Map<String, BridgeInfo> getBridgeInfo() {
        return bridgeInfo;
    }

    private void setBridgeInfo(Map<String, BridgeInfo> bridgeInfo) {
        this.bridgeInfo = bridgeInfo;
    }

    @JsonAnySetter
    private void add(String key, BridgeInfo value) {
        bridgeInfo.put(key, value);
    }

    @JsonAnyGetter
    private Map<String, BridgeInfo> getProperties() {
        return bridgeInfo;
    }

    @Override
    public String toString() {
        return "Bridge [bridgeInfo=" + bridgeInfo + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((bridgeInfo == null) ? 0 : bridgeInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Bridge other = (Bridge) obj;
        if (bridgeInfo == null) {
            if (other.bridgeInfo != null)
                return false;
        } else if (!bridgeInfo.equals(other.bridgeInfo))
            return false;
        return true;
    }
}