/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;

public class SouthboundEvent extends AbstractEvent {
    public enum Type { NODE, ROW };
    private Type type;
    private Node node;
    private String tableName;
    private String uuid;
    private Row row;
    private Object context;
    public SouthboundEvent(Node node, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = Type.NODE;
        this.node = node;
    }
    public SouthboundEvent(Node node, String tableName, String uuid, Row row, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = Type.ROW;
        this.node = node;
        this.tableName = tableName;
        this.uuid = uuid;
        this.row = row;
    }
    public SouthboundEvent(Node node, String tableName, String uuid, Row row, Object context, Action action) {
        super(HandlerType.SOUTHBOUND, action);
        this.type = Type.ROW;
        this.node = node;
        this.tableName = tableName;
        this.uuid = uuid;
        this.row = row;
        this.context = context;
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
    public Row getRow() {
        return row;
    }
    public Object getContext() {
        return context;
    }
    @Override
    public String toString() {
        return "SouthboundEvent [type=" + type + ", action=" + super.getAction() + ", node=" + node + ", tableName=" + tableName
                + ", uuid=" + uuid + ", row=" + row + ", context=" + context.toString() + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        SouthboundEvent other = (SouthboundEvent) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        if (tableName == null) {
            if (other.tableName != null)
                return false;
        } else if (!tableName.equals(other.tableName))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }
}
