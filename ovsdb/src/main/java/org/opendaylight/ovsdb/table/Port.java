package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Port {
    Map<String, PortInfo> portInfo = new HashMap<String, PortInfo>();

      @JsonAnySetter public void add(String key, PortInfo value) {
        portInfo.put(key, value);
      }

      @JsonAnyGetter public Map<String,PortInfo> getProperties() {
        return portInfo;
      }

    @Override
    public String toString() {
        return "Port [portInfo=" + portInfo + "]";
    }
}