/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.Table;

public class SouthboundEvent {
    public enum Type { NODE, ROW };
    public enum Action { ADD, UPDATE, DELETE };
    private Type type;
    private Action action;
    private Node node;
    private String tableName;
    private String uuid;
    private Table<?> row;
    public SouthboundEvent(Node node, Action action) {
        super();
        this.type = Type.NODE;
        this.action = action;
        this.node = node;
    }
    public SouthboundEvent(Node node, String tableName, String uuid, Table<?> row, Action action) {
        super();
        this.type = Type.ROW;
        this.action = action;
        this.node = node;
        this.tableName = tableName;
        this.uuid = uuid;
        this.row = row;
    }
    public Type getType() {
        return type;
    }
    public Action getAction() {
        return action;
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
    public Table<?> getRow() {
        return row;
    }
    @Override
    public String toString() {
        return "SouthboundEvent [type=" + type + ", action=" + action + ", node=" + node + ", tableName=" + tableName
                + ", uuid=" + uuid + ", row=" + row + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
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
        SouthboundEvent other = (SouthboundEvent) obj;
        if (action != other.action)
            return false;
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
        if (type != other.type)
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }
}
