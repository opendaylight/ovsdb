package org.opendaylight.ovsdb.lib.database;


import org.opendaylight.ovsdb.plugin.Connection;
import org.opendaylight.ovsdb.plugin.OvsdbMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OVSInstance {
    private String uuid;

    public OVSInstance(){
        this.uuid = null;
    }

    public OVSInstance(String uuid){
        this.uuid = uuid;
    }

    @SuppressWarnings("unchecked")
    public static OVSInstance monitorOVS(Connection connection){
        List<String> columns = new ArrayList<String>();
        columns.add("_uuid");
        columns.add("bridges");

        Map<String, List<String>> row = new HashMap<String, List<String>>();
        row.put("columns", columns);

        Map<String, Map> tables = new HashMap<String, Map>();
        tables.put("Open_vSwitch", row);

        Object[] params = {"Open_vSwitch", null, tables};

        OvsdbMessage msg = new OvsdbMessage("monitor", params);
        Map<String, Object> monitorResponse = new HashMap<String, Object>();
/*
        try{
            connection.sendMessage(msg);
            monitorResponse = (Map<String, Object>) connection.readResponse(Map.class);
        } catch (Throwable e){
            e.printStackTrace();
        }
*/
        Map<String, Object> vSwitchTable = (Map) monitorResponse.get("Open_vSwitch");
        if(vSwitchTable != null){
            String uuid = (String) vSwitchTable.keySet().toArray()[0];
            return new OVSInstance(uuid);
        }
        return null;
    }

    public String getUuid(){
        return this.uuid;
    }

}
