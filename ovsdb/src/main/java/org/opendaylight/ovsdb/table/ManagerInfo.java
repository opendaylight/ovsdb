package org.opendaylight.ovsdb.table;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManagerInfo {
    @JsonProperty("new")
    private New unique;

    private New getUnique() {
        return unique;
    }

    private void setUnique(New unique) {
        this.unique = unique;
    }

    @Override
    public String toString() {
        return "ManagerInfo [unique=" + unique + "]";
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
        ManagerInfo other = (ManagerInfo) obj;
        if (unique == null) {
            if (other.unique != null)
                return false;
        } else if (!unique.equals(other.unique))
            return false;
        return true;
    }
}