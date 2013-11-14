package org.opendaylight.ovsdb.plugin;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public interface OVSDBConfigService {
    public Status insertRow (Node node, String tableName, String parentUUID, Table<?> row);
    public Status deleteRow (Node node, String tableName, String uuid);
    public Status updateRow (Node node, String rowUUID, Table<?> row);
    public Table<?> getRow(Node node, String tableName, String uuid);
    public List<Table<?>> getRows(Node node, String tableName);
    public List<String> getTables(Node node);
}
