package org.opendaylight.ovsdb.database;


import org.opendaylight.ovsdb.internal.Connection;

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
        return null;
    }
}
