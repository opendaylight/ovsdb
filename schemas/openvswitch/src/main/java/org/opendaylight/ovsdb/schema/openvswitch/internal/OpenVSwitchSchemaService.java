package org.opendaylight.ovsdb.schema.openvswitch.internal;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.ovsdb.lib.schema.typed.SchemaService;

import com.google.common.collect.Maps;

public class OpenVSwitchSchemaService implements SchemaService {
    public static String DATABASE_NAME = "Open_vSwitch";

    private static Map<String, Entry<String, String>> columnToMutate = Maps.newHashMap();
    @Override
    public Entry<String, String> getParentColumnToMutate(String childTabletoInsert, String databaseVersion) {
        return columnToMutate.get(childTabletoInsert);
    }
    private void addParentColumnToMutate(String childTable, String parentTable, String columnName) {
        columnToMutate.put(childTable, new AbstractMap.SimpleEntry<String, String>(parentTable, columnName));
    }

    public OpenVSwitchSchemaService() {
        addParentColumnToMutate("Bridge", "Open_vSwitch", "bridges");
        addParentColumnToMutate("Port", "Bridge", "ports");
        addParentColumnToMutate("Interface", "Port", "interfaces");
        addParentColumnToMutate("SSL", "Open_vSwitch", "ssl");
        addParentColumnToMutate("IPFIX", "Bridge", "ipfix");
        addParentColumnToMutate("sFlow", "Bridge", "sflow");
        addParentColumnToMutate("Flow_Table", "Bridge", "flow_tables");
        addParentColumnToMutate("QoS", "Port", "qos");
        addParentColumnToMutate("NetFlow", "Bridge", "netflow");
        addParentColumnToMutate("Mirror", "Bridge", "mirrors");
        addParentColumnToMutate("Manager", "Open_vSwitch", "manager_options");
        addParentColumnToMutate("Controller", "Bridge", "controller");
    }
}
