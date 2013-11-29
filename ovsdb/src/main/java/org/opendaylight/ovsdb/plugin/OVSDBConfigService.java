package org.opendaylight.ovsdb.plugin;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public interface OVSDBConfigService {
    public StatusWithUuid insertRow (Node node, String tableName, String parentUUID, Table<?> row);
    public Status deleteRow (Node node, String tableName, String rowUUID);
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Table<?> row);
    public String getSerializedRow(Node node, String tableName, String uuid) throws Exception;
    public String getSerializedRows(Node node, String tableName) throws Exception;
    public Table<?> getRow(Node node, String tableName, String uuid) throws Exception;
    public Map<String, Table<?>> getRows(Node node, String tableName) throws Exception;
    public List<String> getTables(Node node) throws Exception;
}
