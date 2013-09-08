package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Interface {

    Map<String, InterfaceInfo> interfaceInfo = new HashMap<String, InterfaceInfo>();

    private Map<String, InterfaceInfo> getInterfaceInfo() {
        return interfaceInfo;
    }

    private void setInterfaceInfo(Map<String, InterfaceInfo> interfaceInfo) {
        this.interfaceInfo = interfaceInfo;
    }

    @JsonAnySetter
    private void add(String key, InterfaceInfo value) {
        interfaceInfo.put(key, value);
    }

    @JsonAnyGetter
    private Map<String, InterfaceInfo> getProperties() {
        return interfaceInfo;
    }

    @Override
    public String toString() {
        return "Interface [interfaceInfo=" + interfaceInfo + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((interfaceInfo == null) ? 0 : interfaceInfo.hashCode());
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
        Interface other = (Interface) obj;
        if (interfaceInfo == null) {
            if (other.interfaceInfo != null)
                return false;
        } else if (!interfaceInfo.equals(other.interfaceInfo))
            return false;
        return true;
    }

}