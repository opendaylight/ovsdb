package org.opendaylight.ovsdb.database;


public class OVSInstance {
    private String uuid;

    public OVSInstance(String uuid){
        this.uuid = uuid;
    }

    public String getUuid(){
        return this.uuid;
    }

}
