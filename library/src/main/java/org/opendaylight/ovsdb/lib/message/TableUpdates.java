/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Capability;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Manager;
import org.opendaylight.ovsdb.lib.table.Mirror;
import org.opendaylight.ovsdb.lib.table.NetFlow;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Qos;
import org.opendaylight.ovsdb.lib.table.Queue;
import org.opendaylight.ovsdb.lib.table.SFlow;
import org.opendaylight.ovsdb.lib.table.SSL;
import org.opendaylight.ovsdb.lib.table.Flow_Sample_Collector_Set;
import org.opendaylight.ovsdb.lib.table.Flow_Table;
import org.opendaylight.ovsdb.lib.table.IPFIX;
import org.opendaylight.ovsdb.lib.table.Table;

import java.util.Map;
import java.util.Set;


public class TableUpdates extends Response {

    Map<Table.Name, TableUpdate> map = Maps.newHashMap();

    public Set<Table.Name> availableUpdates() {
        return map.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Table> TableUpdate<T> getUpdate(Table.Name<T> name) {
        return map.get(name);
    }

    private <T extends Table> void put(Table.Name<T> name, TableUpdate<T> update) {
        map.put(name, update);
    }

    @JsonProperty("Interface")
    public TableUpdate<Interface> getInterfaceUpdate() {
        return getUpdate(Interface.NAME);
    }

    public void setInterfaceUpdate(TableUpdate<Interface> interfaceUpdate) {
        put(Interface.NAME, interfaceUpdate);
    }

    @JsonProperty("Bridge")
    TableUpdate<Bridge> getBridgeUpdate() {
        return getUpdate(Bridge.NAME);
    }

    public void setBridgeUpdate(TableUpdate<Bridge> bridgeUpdate) {
        put(Bridge.NAME, bridgeUpdate);
    }

    @JsonProperty("Port")
    TableUpdate<Port> getPortUpdate() {
        return getUpdate(Port.NAME);
    }

    void setPortUpdate(TableUpdate<Port> portUpdate) {
        put(Port.NAME, portUpdate);
    }

    @JsonProperty("Capability")
    public TableUpdate<Capability> getCapabilityUpdate() {
        return getUpdate(Capability.NAME);
    }

    public void setCapabilityUpdate(TableUpdate<Capability> capabilityUpdate) {
        put(Capability.NAME, capabilityUpdate);
    }

    @JsonProperty("Controller")
    public TableUpdate<Controller> getControllerUpdate() {
        return getUpdate(Controller.NAME);
    }

    public void setControllerUpdate(TableUpdate<Controller> controllerUpdate) {
        put(Controller.NAME, controllerUpdate);
    }

    @JsonProperty("Manager")
    public TableUpdate<Manager> getManagerUpdate() {
        return getUpdate(Manager.NAME);
    }

    public void setManagerUpdate(TableUpdate<Manager> managerUpdate) {
        put(Manager.NAME, managerUpdate);
    }

    @JsonProperty("Mirror")
    public TableUpdate<Mirror> getMirrorUpdate() {
        return getUpdate(Mirror.NAME);
    }

    public void setMirrorUpdate(TableUpdate<Mirror> mirrorUpdate) {
        put(Mirror.NAME, mirrorUpdate);
    }

    @JsonProperty("NetFlow")
    public TableUpdate<NetFlow> getNetFlowUpdate() {
        return getUpdate(NetFlow.NAME);
    }

    public void setNetFlowUpdate(TableUpdate<NetFlow> netFlowUpdate) {
        put(NetFlow.NAME, netFlowUpdate);
    }

    @JsonProperty("Open_vSwitch")
    public TableUpdate<Open_vSwitch> getOpen_vSwitchUpdate() {
        return getUpdate(Open_vSwitch.NAME);
    }

    public void setOpen_vSwitchUpdate(TableUpdate<Open_vSwitch> openVSwitchUpdate) {
        put(Open_vSwitch.NAME, openVSwitchUpdate);
    }

    @JsonProperty("QoS")
    public TableUpdate<Qos> getQosUpdate() {
        return getUpdate(Qos.NAME);
    }

    public void setQosUpdate(TableUpdate<Qos> qosUpdate) {
        put(Qos.NAME, qosUpdate);
    }

    @JsonProperty("Queue")
    public TableUpdate<Queue> getQueueUpdate() {
        return getUpdate(Queue.NAME);
    }

    public void setQueueUpdate(TableUpdate<Queue> queueUpdate) {
        put(Queue.NAME, queueUpdate);
    }

    @JsonProperty("sFlow")
    public TableUpdate<SFlow> getSFlowUpdate() {
        return getUpdate(SFlow.NAME);
    }

    public void setSFlowUpdate(TableUpdate<SFlow> sFlowUpdate) {
        put(SFlow.NAME, sFlowUpdate);
    }

    @JsonProperty("SSL")
    public TableUpdate<SSL> getSSLUpdate() {
        return getUpdate(SSL.NAME);
    }

    public void setSSLUpdate(TableUpdate<SSL> sslUpdate) {
        put(SSL.NAME, sslUpdate);
    }

    @JsonProperty("Flow_Table")
    public TableUpdate<Flow_Table> getFlow_TableUpdate() {
        return getUpdate(Flow_Table.NAME);
    }

    public void setFlow_TableUpdate(TableUpdate<Flow_Table> Flow_TableUpdate) {
        put(Flow_Table.NAME, Flow_TableUpdate);
    }

    @JsonProperty("Flow_Sample_Collector_Set")
    public TableUpdate<Flow_Sample_Collector_Set> getFlow_Sample_Collector_SetUpdate() {
        return getUpdate(Flow_Sample_Collector_Set.NAME);
    }

    public void setFlow_Sample_Collector_SetUpdate(TableUpdate<Flow_Sample_Collector_Set> Flow_Sample_Collector_SetUpdate) {
        put(Flow_Sample_Collector_Set.NAME, Flow_Sample_Collector_SetUpdate);
    }

    @JsonProperty("IPFIX")
    public TableUpdate<IPFIX> getIPFIXUpdate() {
        return getUpdate(IPFIX.NAME);
    }

    public void setIPFIXUpdate(TableUpdate<IPFIX> IPFIXUpdate) {
        put(IPFIX.NAME, IPFIXUpdate);
    }

    public TableUpdate getUpdate(GenericTableSchema table) {
        //todo Horrible just for time being, before this whole thing is refactored.
        for (Map.Entry<Table.Name, TableUpdate> s : this.map.entrySet()) {
            if (table.getName().equals(s.getKey().getName())) {
                return s.getValue();
            }
        }
        return null;
    }
}
