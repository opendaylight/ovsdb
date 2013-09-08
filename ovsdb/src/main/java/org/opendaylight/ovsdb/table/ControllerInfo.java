package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ControllerInfo {

    @JsonProperty("new")
    private New unique;

    private New getNew() {
        return unique;
    }

    private void setNew(New unique) {
        this.unique = unique;
    }

    @Override
    public String toString() {
        return "ControllerInfo [new="
                + unique + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((unique == null) ? 0 : unique.hashCode());
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
        ControllerInfo other = (ControllerInfo) obj;
        if (unique == null) {
            if (other.unique != null)
                return false;
        } else if (!unique.equals(other.unique))
            return false;
        return true;
    }
}