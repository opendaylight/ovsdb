package org.opendaylight.ovsdb.plugin;
import java.util.Map;

import com.google.common.collect.Maps;
public class OvsVswitchdSchemaConstants {
    public static String DATABASE_NAME = "Open_vSwitch";

    private static Map<String, String[]> columnToMutate = Maps.newHashMap();
    public static String[] getParentColumnToMutate(String childTabletoInsert) {
        return columnToMutate.get(childTabletoInsert);
    }
    private static void addParentColumnToMutate(String childTable, String parentTable, String columnName) {
        String[] parentColumn = {parentTable, columnName};
        columnToMutate.put(childTable, parentColumn);
    }

    static {
        addParentColumnToMutate("Bridge", "Open_vSwitch", "bridges");
        addParentColumnToMutate("Port", "Bridge", "ports");
        addParentColumnToMutate("Interface", "Port", "interfaces");
        addParentColumnToMutate("SSL", "Open_vSwitch", "ssl");
        addParentColumnToMutate("IPFIX", "Bridge", "ipfix");
        addParentColumnToMutate("SFlow", "Bridge", "sflow");
        addParentColumnToMutate("Qos", "Port", "qos");
        addParentColumnToMutate("Netflow", "Bridge", "netflow");
        addParentColumnToMutate("Mirror", "Bridge", "mirrors");
        addParentColumnToMutate("Manager", "Open_vSwitch", "manager_options");
    }
}
