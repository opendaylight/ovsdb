package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Port {

    Map<String, PortInfo> portInfo = new HashMap<String, PortInfo>();

    private Map<String, PortInfo> getPortInfo() {
        return portInfo;
    }

    private void setPortInfo(Map<String, PortInfo> portInfo) {
        this.portInfo = portInfo;
    }

    @JsonAnySetter
    private void add(String key, PortInfo value) {
        portInfo.put(key, value);
    }

    @JsonAnyGetter
    private Map<String, PortInfo> getProperties() {
        return portInfo;
    }

    @Override
    public String toString() {
        return "Port [portInfo=" + portInfo + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((portInfo == null) ? 0 : portInfo.hashCode());
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
        Port other = (Port) obj;
        if (portInfo == null) {
            if (other.portInfo != null)
                return false;
        } else if (!portInfo.equals(other.portInfo))
            return false;
        return true;
    }
}