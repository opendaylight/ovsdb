/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.lib.database;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static Map<String, OVSBridge> monitorBridge(){
        List<String> columns = new ArrayList<String>();
        columns.add("_uuid");
        columns.add("name");

        Map<String, List<String>> row = new HashMap<String, List<String>>();
        row.put("columns", columns);

        Map<String, Map> tables = new HashMap<String, Map>();
        tables.put("Bridge", row);

        Object[] params = {"Open_vSwitch", null, tables};

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
