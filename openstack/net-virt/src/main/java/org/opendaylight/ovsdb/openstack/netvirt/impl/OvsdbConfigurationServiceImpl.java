package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbPluginException;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusWithUuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class OvsdbConfigurationServiceImpl implements OvsdbConfigurationService {
    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row) {
        return null;
    }

    @Override
    public Status updateRow(Node node, String tableName, String parentUuid, String rowUuid, Row row) {
        return null;
    }

    @Override
    public Status deleteRow(Node node, String tableName, String rowUuid) {
        return null;
    }

    @Override
    public Row getRow(Node node, String tableName, String uuid) {
        return null;
    }

    @Override
    public Row<GenericTableSchema> getRow(Node node, String databaseName, String tableName, UUID uuid) throws OvsdbPluginException {
        return null;
    }

    @Override
    public ConcurrentMap<String, Row> getRows(Node node, String tableName) {
        return null;
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node, String databaseName, String tableName) throws OvsdbPluginException {
        return null;
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node, String databaseName, String tableName, String fiqlQuery) throws OvsdbPluginException {
        return null;
    }

    @Override
    public List<String> getTables(Node node) {
        return null;
    }

    @Override
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass) {
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row) {
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass) {
        return null;
    }

    @Override
    public ConcurrentMap<String, OvsdbTerminationPointAugmentation> getInterfaces(Node node) {
        return null;
    }
}
