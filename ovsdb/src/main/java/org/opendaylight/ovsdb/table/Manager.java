package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Manager {

    Map<String, ManagerInfo> managerInfo = new HashMap<String, ManagerInfo>();

    private Map<String, ManagerInfo> getManagerInfo() {
        return managerInfo;
    }

    private void setManagerInfo(Map<String, ManagerInfo> managerInfo) {
        this.managerInfo = managerInfo;
    }

    @JsonAnySetter
    private void add(String key, ManagerInfo value) {
        managerInfo.put(key, value);
    }

    @JsonAnyGetter
    private Map<String, ManagerInfo> getProperties() {
        return managerInfo;
    }

    @Override
    public String toString() {
        return "Manager [managerInfo=" + managerInfo + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((managerInfo == null) ? 0 : managerInfo.hashCode());
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
        Manager other = (Manager) obj;
        if (managerInfo == null) {
            if (other.managerInfo != null)
                return false;
        } else if (!managerInfo.equals(other.managerInfo))
            return false;
        return true;
    }
}