package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class New {

    @JsonProperty("interfaces")
    private List<String> interfaces;
    @JsonProperty("name")
    private String name;
    @JsonProperty("fake_bridge")
    private boolean fake_bridge;
    @JsonProperty("type")
    private String type;
    @JsonProperty("ovs_version")
    private String ovs_version;
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("target")
    private String target;
    @JsonProperty("is_connected")
    private boolean is_connected;

    private List<String> getInterfaces() {
        return interfaces;
    }

    private void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    private String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    private boolean isFake_bridge() {
        return fake_bridge;
    }

    private void setFake_bridge(boolean fake_bridge) {
        this.fake_bridge = fake_bridge;
    }

    private String getType() {
        return type;
    }

    private void setType(String type) {
        this.type = type;
    }

    private String getOvs_version() {
        return ovs_version;
    }

    private void setOvs_version(String ovs_version) {
        this.ovs_version = ovs_version;
    }

    private String getUuid() {
        return uuid;
    }

    private void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private String getTarget() {
        return target;
    }

    private void setTarget(String target) {
        this.target = target;
    }

    private boolean isIs_connected() {
        return is_connected;
    }

    private void setIs_connected(boolean is_connected) {
        this.is_connected = is_connected;
    }

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (fake_bridge ? 1231 : 1237);
        result = prime * result
                + ((interfaces == null) ? 0 : interfaces.hashCode());
        result = prime * result + (is_connected ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((ovs_version == null) ? 0 : ovs_version.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
        New other = (New) obj;
        if (fake_bridge != other.fake_bridge)
            return false;
        if (interfaces == null) {
            if (other.interfaces != null)
                return false;
        } else if (!interfaces.equals(other.interfaces))
            return false;
        if (is_connected != other.is_connected)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (ovs_version == null) {
            if (other.ovs_version != null)
                return false;
        } else if (!ovs_version.equals(other.ovs_version))
            return false;
        if (target == null) {
            if (other.target != null)
                return false;
        } else if (!target.equals(other.target))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }
}

