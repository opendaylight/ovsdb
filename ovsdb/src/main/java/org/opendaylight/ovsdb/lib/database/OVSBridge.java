package org.opendaylight.ovsdb.lib.database;


import org.opendaylight.ovsdb.plugin.Connection;
import org.opendaylight.ovsdb.plugin.OvsdbMessage;

import java.util.*;

public class OVSBridge {

    private String uuid;
    private String name;

    public OVSBridge(String uuid, String name){
        this.uuid = uuid;
        this.name = name;
    }

    public String getUuid(){
        return this.uuid;
    }

    public String getName(){
        return this.name;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, OVSBridge> monitorBridge(Connection connection){
        List<String> columns = new ArrayList<String>();
        columns.add("_uuid");
        columns.add("name");

        Map<String, List<String>> row = new HashMap<String, List<String>>();
        row.put("columns", columns);

        Map<String, Map> tables = new HashMap<String, Map>();
        tables.put("Bridge", row);

        Object[] params = {"Open_vSwitch", null, tables};

        OvsdbMessage msg = new OvsdbMessage("monitor", params);
        Map<String, Object> monitorResponse = new HashMap<String, Object>();

        Map<String, Object> bridgeTable = (Map) monitorResponse.get("Bridge");

        Object[] uuidObjects = bridgeTable.keySet().toArray();
        String[] uuids = Arrays.copyOf(uuidObjects, uuidObjects.length, String[].class);

        Map<String, OVSBridge> result = new HashMap<String, OVSBridge>();

        for(String uuid : uuids){
            Map<String, Object> newRow = (Map) bridgeTable.get(uuid);
            Map<String, Object> newColumns = (Map) newRow.get("new");
            String name = (String) newColumns.get("name");
            result.put(name, new OVSBridge(uuid, name));
        }

        return result;
    }
}
