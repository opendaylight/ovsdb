package org.opendaylight.ovsdb.database;

import java.util.*;

import org.opendaylight.ovsdb.internal.Connection;
import org.opendaylight.ovsdb.internal.OvsdbMessage;

public class OVSPort {
    private String uuid;
    private String name;
    private static String bridgeUuid;

    public OVSPort(String uuid, String name){
        this.uuid = uuid;
        this.name = name;
    }

    public String getBridgeUuid(){
        return bridgeUuid;
    }

    public String getUuid(){
        return this.uuid;
    }

    public String getName(){
        return this.name;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, OVSPort> monitorPort(Connection connection){

        //Bridge Table Query
        List<String> columnsBr = new ArrayList<String>();
        columnsBr.add("_uuid");
        columnsBr.add("name");
        columnsBr.add("ports");
        Map<String, List<String>> rowBr = new HashMap<String, List<String>>();
        rowBr.put("columns", columnsBr);
        Map<String, Map> tablesBr = new HashMap<String, Map>();
        tablesBr.put("Bridge", rowBr);

        //Ports Table Query
        List<String> columnsPort = new ArrayList<String>();
        columnsPort.add("_uuid");
        columnsPort.add("name");
        Map<String, List<String>> rowPort = new HashMap<String, List<String>>();
        rowPort.put("columns", columnsPort);
        Map<String, Map> tablesPort = new HashMap<String, Map>();
        tablesBr.put("Port", rowPort);

        Object[] params = {"Open_vSwitch", null, tablesBr};
        OvsdbMessage msg = new OvsdbMessage("monitor", params);

        Map<String, Object> monitorResponse = new HashMap<String, Object>();

        try{
            connection.sendMessage(msg);
            monitorResponse = (Map<String, Object>) connection.readResponse(Map.class);
        } catch (Throwable e){
            e.printStackTrace();
        }

        //This section is purely for getting the bridge UUID and setting it to the local var to be retrieved later
        Map<String, Object> bridgeTable = (Map) monitorResponse.get("Bridge");

        Object[] uuidObjectsBridge = bridgeTable.keySet().toArray();
        String[] uuidsBridge = Arrays.copyOf(uuidObjectsBridge, uuidObjectsBridge.length, String[].class);

        for(String uuid : uuidsBridge){
            Map<String, Object> newRow = (Map) bridgeTable.get(uuid);
            Map<String, Object> newColumns = (Map) newRow.get("new");
            String name = (String) newColumns.get("name");
            bridgeUuid = uuid;
        }

        Map<String, OVSPort> result = new HashMap<String, OVSPort>();
        Map<String, Object> portTable = (Map) monitorResponse.get("Port");

        Object[] uuidObjectsPort = portTable.keySet().toArray();
        String[] uuidsPort = Arrays.copyOf(uuidObjectsPort, uuidObjectsPort.length, String[].class);

        for(String uuid : uuidsPort){
            Map<String, Object> newRow = (Map) portTable.get(uuid);
            Map<String, Object> newColumns = (Map) newRow.get("new");
            String name = (String) newColumns.get("name");
            result.put(name, new OVSPort(uuid, name));
        }

        return result;
    }
}
