package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class New {
    @JsonProperty("interfaces")
    List<String> interfaces;
    @JsonProperty("name")
    String name;
    @JsonProperty("fake_bridge")
    boolean fake_bridge;
    @JsonProperty("type")
    String type;
    @JsonProperty("ovs_version")
    String ovs_version;
    @JsonProperty("uuid")
    String uuid;
    @JsonProperty("target")
    String target;
    @JsonProperty("is_connected")
    boolean is_connected;

    @Override
    public String toString() {
        return "New{" +
                "interfaces=" + interfaces +
                ", name='" + name + '\'' +
                ", fake_bridge=" + fake_bridge +
                ", type='" + type + '\'' +
                ", ovs_version='" + ovs_version + '\'' +
                ", uuid='" + uuid + '\'' +
                ", target='" + target + '\'' +
                ", is_connected=" + is_connected +
                '}';
    }
}

