package org.opendaylight.ovsdb.plugin;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public interface OVSDBInventoryListener {
    public void nodeAdded(Node node);
    public void nodeRemoved(Node node);
    public void rowAdded(Node node, String tableName, String uuid, Table<?> row);
    public void rowUpdated(Node node, String tableName, String uuid, Table<?> row);
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row);
}
