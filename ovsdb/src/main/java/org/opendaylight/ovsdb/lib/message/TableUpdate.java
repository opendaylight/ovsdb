package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.table.internal.Table;


public  class TableUpdate<T extends Table>  {
    /*This could have been done as a map, but doing so would expose the inner wrapper class in type signature*/

    Map<String, Row<T>> map = Maps.newHashMap();

    @JsonAnyGetter
    public Row<T> get(String rowId) {
        return map.get(rowId);
    }

    @JsonAnySetter
    public void set(String rowId, Row<T> value) {
        map.put(rowId, value);
        value.setId(rowId);
    }

    public Collection<Row<T>> getRows() {
        return map.values();
    }

    @Override
    public String toString() {
        return "TableUpdate [map=" + map + "]";
    }

    public static class Row<T> {

        @JsonIgnore
        String id;

        @JsonProperty("new")
        T _new;
        T old;

        public String getId() {
            return id;
        }

        public T getNew() {
            return _new;
        }

        public void setNew(T neww) {
            this._new = neww;
        }

        public T getOld() {
            return old;
        }

        public void setOld(T old) {
            this.old = old;
        }

        void setId(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Row{" +
                    "id='" + id + '\'' +
                    ", _new=" + _new.toString() +
                    '}';
        }

    }
}
