package org.opendaylight.ovsdb.lib.meta;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.Insert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author araveendrann
 */
public class TableSchema<E extends TableSchema<E>> {
    protected static final Logger logger = LoggerFactory.getLogger(TableSchema.class);
    private String name;
    private Map<String, ColumnSchema> columns;

    public TableSchema() {
    }

    public TableSchema(String name, Map<String, ColumnSchema> columns) {
        this.name = name;
        this.columns = columns;
    }

    public static TableSchema fromJson(String tableName, JsonNode json) {

        if (!json.isObject() || !json.has("columns")) {
            //todo specific types of exception
            throw new RuntimeException("bad tableschema root, expected \"columns\" as child");
        }

        Map<String, ColumnSchema> columns = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("columns").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> column = iter.next();
            logger.debug("%s:%s", tableName, column.getKey());
            columns.put(column.getKey(), ColumnSchema.fromJson(column.getKey(), column.getValue()));
        }

       TableSchema tableSchema = new TableSchema(tableName, columns);
       return tableSchema;
    }

    public <E extends TableSchema<E>> E as(Class<E> clazz) {
        try {
            Constructor<E> e = clazz.getConstructor(TableSchema.class);
            return e.newInstance(this);
        } catch (Exception e) {
            throw new RuntimeException("exception constructing instance of clazz " + clazz, e);
        }
    }

    public Insert<E> insert() {
        return new Insert<>(this);
    }



    public <D> ColumnSchema<E, D> column(String column) {
        //todo exception handling
        return columns.get(column);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class AnyTableSchema extends TableSchema<AnyTableSchema>{}
}
