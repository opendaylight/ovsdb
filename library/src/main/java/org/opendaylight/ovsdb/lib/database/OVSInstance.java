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
    public static OVSInstance monitorOVS(){
        List<String> columns = new ArrayList<String>();
        columns.add("_uuid");
        columns.add("bridges");

        Map<String, List<String>> row = new HashMap<String, List<String>>();
        row.put("columns", columns);

        Map<String, Map> tables = new HashMap<String, Map>();
        tables.put("Open_vSwitch", row);

        Object[] params = {"Open_vSwitch", null, tables};

        Map<String, Object> monitorResponse = new HashMap<String, Object>();

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
