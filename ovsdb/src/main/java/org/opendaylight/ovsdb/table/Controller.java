package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Controller {

    Map<String, ControllerInfo> controllerInfo = new HashMap<String, ControllerInfo>();

    private Map<String, ControllerInfo> getControllerInfo() {
        return controllerInfo;
    }

    private void setControllerInfo(Map<String, ControllerInfo> controllerInfo) {
        this.controllerInfo = controllerInfo;
    }

    @JsonAnySetter
    private void add(String key, ControllerInfo value) {
        controllerInfo.put(key, value);
    }

    @JsonAnyGetter
    private Map<String, ControllerInfo> getProperties() {
        return controllerInfo;
    }

    @Override
    public String toString() {
        return "Controller [controllerInfo=" + controllerInfo + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((controllerInfo == null) ? 0 : controllerInfo.hashCode());
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
        Controller other = (Controller) obj;
        if (controllerInfo == null) {
            if (other.controllerInfo != null)
                return false;
        } else if (!controllerInfo.equals(other.controllerInfo))
            return false;
        return true;
    }
}