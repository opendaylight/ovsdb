/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SouthboundEvent extends AbstractEvent {
    public enum Type { NODE, OPENVSWITCH, BRIDGE, CONTROLLER, PORT }
    private Type type;
    private Node node;
    private String tableName;
    private String uuid;
    private Object context;
    private DataObject augmentationData;
    private OvsdbBridgeAugmentation bridge;
    private OvsdbTerminationPointAugmentation port;
    private String portName;
    public SouthboundEvent(Node node, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = Type.NODE;
        this.node = node;
    }
    public SouthboundEvent(Node node, OvsdbBridgeAugmentation bridge, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = Type.BRIDGE;
        this.node = node;
        this.bridge = bridge;
    }
    public SouthboundEvent(Node node, OvsdbTerminationPointAugmentation port, String portName, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = Type.PORT;
        this.node = node;
        this.port = port;
        this.portName = portName;
    }

    public SouthboundEvent(Node node, Type type, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = type;
        this.node = node;
    }

    public SouthboundEvent(Node node, DataObject resourceAugmentationData, Type type, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = type;
        this.node = node;
        this.augmentationData = resourceAugmentationData;
    }


    public Type getType() {
        return type;
    }
    public Node getNode() {
        return node;
    }
    public String getTableName() {
        return tableName;
    }
    public String getUuid() {
        return uuid;
    }
    public Object getContext() {
        return context;
    }
    public OvsdbBridgeAugmentation getBridge() {
        return bridge;
    }
    public OvsdbTerminationPointAugmentation getPort() {
        return port;
    }
    public String getPortName() {
        return portName;
    }

    public DataObject getAugmentationData() {
        return augmentationData;
    }

    @Override
    public String toString() {
        //if (type == Type.NODE) {
            return "SouthboundEvent [type=" + type
                    + ", action=" + super.getAction()
                    + ", augmentationData=" + augmentationData
                    + ", node=" + node + "]";
        //} else {
        //    return "SouthboundEvent [type=" + type + "]";
        //}
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        result = prime * result + ((augmentationData == null) ? 0 : augmentationData.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        SouthboundEvent other = (SouthboundEvent) obj;
        if (node == null) {
            if (other.node != null) {
                return false;
            }
        } else if (!node.equals(other.node)) {
            return false;
        }
        if (tableName == null) {
            if (other.tableName != null) {
                return false;
            }
        } else if (!tableName.equals(other.tableName)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }
}
