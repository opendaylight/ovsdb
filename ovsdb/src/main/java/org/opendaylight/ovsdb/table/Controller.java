package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Controller {
    Map<String, ControllerInfo> controllerInfo = new HashMap<String, ControllerInfo>();

    @JsonAnySetter public void add(String key, ControllerInfo value) {
        controllerInfo.put(key, value);
    }
    @JsonAnyGetter public Map<String,ControllerInfo> getProperties() {
        return controllerInfo;
    }
    @Override
    public String toString() {
        return "Controller [controllerInfo=" + controllerInfo + "]";
    }
}