package org.opendaylight.ovsdb.plugin;
import java.util.Map;

import com.google.common.collect.Maps;
public class OvsVswitchdSchemaConstants {
    public static String DATABASE_NAME = "Open_vSwitch";

    private static final String OVSDB_AUTOCONFIGURECONTROLLER = "ovsdb.autoconfigurecontroller";
    private static final boolean defaultAutoConfigureController = true;
    private static boolean autoConfigureController = defaultAutoConfigureController;

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
        addParentColumnToMutate("sFlow", "Bridge", "sflow");
        addParentColumnToMutate("Flow_Table", "Bridge", "flow_tables");
        addParentColumnToMutate("QoS", "Port", "qos");
        addParentColumnToMutate("Queue", "Qos", "queues");
        addParentColumnToMutate("NetFlow", "Bridge", "netflow");
        addParentColumnToMutate("Mirror", "Bridge", "mirrors");
        addParentColumnToMutate("Manager", "Open_vSwitch", "manager_options");
        addParentColumnToMutate("Controller", "Bridge", "controller");
        // Keep the default value if the property is not set
        if (System.getProperty(OVSDB_AUTOCONFIGURECONTROLLER) != null)
            autoConfigureController = Boolean.getBoolean(OVSDB_AUTOCONFIGURECONTROLLER);
    }

    public static boolean shouldConfigureController (String databaseName, String tableName) {
        if (autoConfigureController && databaseName.equals(DATABASE_NAME) && tableName.equals("Bridge")) return true;
        return false;
    }
}
