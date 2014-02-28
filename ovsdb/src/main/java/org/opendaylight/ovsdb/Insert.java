package org.opendaylight.ovsdb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.meta.ColumnSchema;
import org.opendaylight.ovsdb.lib.meta.TableSchema;

import java.util.Map;

/**
 * @author Ashwin Raveendran
 */
public class Insert<E extends TableSchema<E>> extends Operation<E> {

    public static final String INSERT = "insert";

    String uuid;

    @JsonProperty("uuid-name")
    private String uuidName;

    private Map<String, Object> row = Maps.newHashMap();

    public Insert on(TableSchema schema){
        this.setTableSchema(schema);
        return this;
    }

    public Insert withId(String name) {
        this.uuidName = name;
        this.setOp(INSERT);
        return this;
    }


    public Insert(TableSchema<E> schema) {
        super(schema, INSERT);
    }

    public <D, C extends TableSchema<C>> Insert<E> value(ColumnSchema<C, D> columnSchema, D value) {
        row.put(columnSchema.getName(), value);
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuidName() {
        return uuidName;
    }

    public void setUuidName(String uuidName) {
        this.uuidName = uuidName;
    }

    public Map<String, Object> getRow() {
        return row;
    }

    public void setRow(Map<String, Object> row) {
        this.row = row;
    }



}
