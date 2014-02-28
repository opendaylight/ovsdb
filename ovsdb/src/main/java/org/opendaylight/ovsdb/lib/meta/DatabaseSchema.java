package org.opendaylight.ovsdb.lib.meta;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.OpenVswitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author araveendrann
 */
public class DatabaseSchema {
    public static Logger logger = LoggerFactory.getLogger(DatabaseSchema.class);

    public static String OPEN_VSWITCH_SCHEMA_NAME = "Open_vSwitch";

    public Map<String, TableSchema> tables;

    public DatabaseSchema(Map<String, TableSchema> tables) {
        this.tables = tables;
    }


    public static DatabaseSchema fromJson(JsonNode json) {
        if (!json.isObject() || !json.has("tables")) {
            //todo specific types of exception
            throw new RuntimeException("bad databaseschema root, expected \"tables\" as child");
        }

        Map<String, TableSchema> tables = new HashMap<>();
        //Iterator<Map.Entry<String,JsonNode>> fields = json.fields();
        for(Iterator<Map.Entry<String,JsonNode>> iter = json.get("tables").fields(); iter.hasNext();) {
            Map.Entry<String, JsonNode> table = iter.next();
            logger.debug("Read schema for table[{}]:{}" , table.getKey(), table.getValue());

            tables.put(table.getKey(), TableSchema.fromJson(table.getKey(), table.getValue()));
        }

        return new DatabaseSchema(tables);
    }

    public OpenVswitch.Transaction beginTransaction() {
        return new OpenVswitch.Transaction(this);
    }

    public <E extends TableSchema<E>> TableSchema<E> table(String tableName) {
        //todo : error handling
        return tables.get(tableName);
    }

    public <E extends TableSchema<E>> E table(String tableName, Class<E> clazz) {
        TableSchema<E> table = table(tableName);
        return table.as(clazz);
    }
}
