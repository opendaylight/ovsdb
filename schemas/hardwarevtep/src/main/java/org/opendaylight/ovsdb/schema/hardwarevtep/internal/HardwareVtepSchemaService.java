package org.opendaylight.ovsdb.schema.hardwarevtep.internal;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.ovsdb.lib.schema.typed.SchemaService;

import com.google.common.collect.Maps;

public class HardwareVtepSchemaService implements SchemaService {
    public static String DATABASE_NAME = "hardware_vtep";

    private static Map<String, Entry<String, String>> columnToMutate = Maps.newHashMap();
    @Override
    public Entry<String, String> getParentColumnToMutate(String childTabletoInsert, String databaseVersion) {
        return columnToMutate.get(childTabletoInsert);
    }
    private void addParentColumnToMutate(String childTable, String parentTable, String columnName) {
        columnToMutate.put(childTable, new AbstractMap.SimpleEntry<String, String>(parentTable, columnName));
    }

    public HardwareVtepSchemaService() {
        addParentColumnToMutate("Manager", "Global", "managers");
        addParentColumnToMutate("Physical_Switch", "Global", "switches");
        addParentColumnToMutate("Physical_Port", "Physical_Switch", "ports");
    }
}
