package org.opendaylight.ovsdb.openstack.netvirt.api;

public class OvsdbPluginException extends RuntimeException {
    public OvsdbPluginException(String message){
        super(message);
    }

    public OvsdbPluginException(String message, Throwable cause){
        super(message, cause);
    }
}
