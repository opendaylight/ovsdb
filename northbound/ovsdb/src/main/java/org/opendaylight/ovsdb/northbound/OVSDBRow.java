package org.opendaylight.ovsdb.northbound;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Capability;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Manager;
import org.opendaylight.ovsdb.lib.table.Mirror;
import org.opendaylight.ovsdb.lib.table.NetFlow;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.Qos;
import org.opendaylight.ovsdb.lib.table.Queue;
import org.opendaylight.ovsdb.lib.table.SFlow;
import org.opendaylight.ovsdb.lib.table.SSL;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class OVSDBRow {
    String parent_uuid;
    /*
     * MINIMAL_CLASS Directive expects a leading "." character on the class name and is lame.
     * Hence going with NAME directive even though it calls for SubTypes.
     * Since we are using fixed table types for the Hydrogen release, this is acceptable.
     * When we move towards Schema driven table definition, this is anyways not required.

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.MINIMAL_CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@class")
    */

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value=Bridge.class, name="Bridge"),
        @JsonSubTypes.Type(value=Capability.class, name="Capbility"),
        @JsonSubTypes.Type(value=Controller.class, name="Controller"),
        @JsonSubTypes.Type(value=Interface.class, name="Interface"),
        @JsonSubTypes.Type(value=Manager.class, name="Manager"),
        @JsonSubTypes.Type(value=Mirror.class, name="Mirror"),
        @JsonSubTypes.Type(value=NetFlow.class, name="NetFlow"),
        @JsonSubTypes.Type(value=Open_vSwitch.class, name="Open_vSwitch"),
        @JsonSubTypes.Type(value=Port.class, name="Port"),
        @JsonSubTypes.Type(value=Qos.class, name="QoS"),
        @JsonSubTypes.Type(value=Queue.class, name="Queue"),
        @JsonSubTypes.Type(value=SFlow.class, name="sFlow"),
        @JsonSubTypes.Type(value=SSL.class, name="SSL")
        })
    Table row;

    public OVSDBRow() {
    }
    public String getParent_uuid() {
        return parent_uuid;
    }
    public void setParent_uuid(String parent_uuid) {
        this.parent_uuid = parent_uuid;
    }
    public Table getRow() {
        return row;
    }
    public void setRow(Table row) {
        this.row = row;
    }
}
