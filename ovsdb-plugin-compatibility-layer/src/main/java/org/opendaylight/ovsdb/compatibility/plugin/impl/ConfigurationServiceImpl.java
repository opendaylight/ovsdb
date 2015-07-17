/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.compatibility.plugin.api.NodeUtils;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.compatibility.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.compatibility.plugin.error.OvsdbPluginException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

/**
 * This is a proxy class for ovsdb plugin's OvsdbConfigurationService class
 * It just forward the call to OvsdbConfigurationService instance and pass
 * back the response to the caller.
 *
 * @author Anil Vishnoi (vishnoianil@gmail.com)
 *
 */
public class ConfigurationServiceImpl implements OvsdbConfigurationService
{
    private volatile org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService pluginOvsdbConfigurationService;

    void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    public void setOvsdbConfigurationService(org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService pluginOvsdbConfigurationService){
        this.pluginOvsdbConfigurationService = pluginOvsdbConfigurationService;
    }

    public void unsetOvsdbConfigurationService(org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService pluginOvsdbConfigurationService){
        this.pluginOvsdbConfigurationService = null;
    }


    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row) {
        return StatusConvertorUtil.convertOvsdbStatusWithUuidToCompLayerStatusWithUuid(pluginOvsdbConfigurationService.insertRow(NodeUtils.getMdsalNode(node), tableName, parentUuid, row));
    }

    @Override
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Row row) {
        return StatusConvertorUtil
                .convertOvsdbStatusToSalStatus(pluginOvsdbConfigurationService
                        .updateRow(NodeUtils.getMdsalNode(node), tableName, parentUUID, rowUUID, row));
    }

    @Override
    public Status deleteRow(Node node, String tableName, String uuid) {
        return StatusConvertorUtil
                .convertOvsdbStatusToSalStatus(pluginOvsdbConfigurationService.
                        deleteRow(NodeUtils.getMdsalNode(node), tableName, uuid));
    }

    @Override
    public ConcurrentMap<String, Row> getRows(Node node, String tableName) {
        return pluginOvsdbConfigurationService.getRows(NodeUtils.getMdsalNode(node), tableName);
    }

    @Override
    public Row getRow(Node node, String tableName, String uuid) {
        return pluginOvsdbConfigurationService.getRow(NodeUtils.getMdsalNode(node), tableName, uuid);
    }

    @Override
    public List<String> getTables(Node node) {
        return pluginOvsdbConfigurationService.getTables(NodeUtils.getMdsalNode(node));
    }

    @Override
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException {
        return pluginOvsdbConfigurationService.setOFController(NodeUtils.getMdsalNode(node), bridgeUUID);
    }

    @Override
    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass) {
        return pluginOvsdbConfigurationService.getTableName(NodeUtils.getMdsalNode(node), typedClass);
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row) {
        return pluginOvsdbConfigurationService.getTypedRow(NodeUtils.getMdsalNode(node), typedClass, row);
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass) {
        return pluginOvsdbConfigurationService.createTypedRow(NodeUtils.getMdsalNode(node), typedClass);
    }

    @Override
    public UUID insertRow(Node node, String databaseName, String tableName, String parentTable, UUID parentUuid,
                          String parentColumn, Row<GenericTableSchema> row) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService
                .insertRow(NodeUtils.getMdsalNode(node), databaseName, tableName, parentTable, parentUuid,
                        parentColumn, row);
    }

    @Override
    public UUID insertRow(Node node, String databaseName, String tableName,
            UUID parentRowUuid, Row<GenericTableSchema> row)
            throws OvsdbPluginException {
        return this.insertRow(node, databaseName, tableName, null, parentRowUuid, null, row);
    }

    @Override
    public Row<GenericTableSchema> insertTree(Node node, String databaseName, String tableName, String parentTable, UUID parentUuid,
                                              String parentColumn, Row<GenericTableSchema> row) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService
                .insertTree(NodeUtils.getMdsalNode(node), databaseName, tableName, parentTable,
                        parentUuid, parentColumn, row);
    }

    @Override
    public Row<GenericTableSchema> insertTree(Node node, String databaseName,
            String tableName, UUID parentRowUuid, Row<GenericTableSchema> row)
            throws OvsdbPluginException {
        return this.insertTree(node, databaseName, tableName, null, parentRowUuid, null, row);
    }

    @Override
    public Row<GenericTableSchema> updateRow(Node node, String databaseName,
            String tableName, UUID rowUuid, Row<GenericTableSchema> row,
            boolean overwrite) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService
                .updateRow(NodeUtils.getMdsalNode(node), databaseName, tableName, rowUuid, row, overwrite);
    }

    @Override
    public void deleteRow(Node node, String databaseName, String tableName, String parentTable, UUID parentRowUuid,
            String parentColumn, UUID rowUuid) throws OvsdbPluginException {
        pluginOvsdbConfigurationService
                .deleteRow(NodeUtils.getMdsalNode(node), databaseName, tableName, parentTable,
                        parentRowUuid, parentColumn, rowUuid);
    }

    @Override
    public void deleteRow(Node node, String databaseName, String tableName, UUID rowUuid) throws OvsdbPluginException {
        this.deleteRow(node, databaseName, tableName, null, null, null, rowUuid);
    }

    @Override
    public Row<GenericTableSchema> getRow(Node node, String databaseName,
            String tableName, UUID uuid) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService
                .getRow(NodeUtils.getMdsalNode(node), databaseName, tableName, uuid);
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node,
            String databaseName, String tableName) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService
                .getRows(NodeUtils.getMdsalNode(node), databaseName, tableName);
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node,
            String databaseName, String tableName, String fiqlQuery)
            throws OvsdbPluginException {
        return this.getRows(node, databaseName, tableName);
    }

    @Override
    public List<String> getTables(Node node, String databaseName) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService.getTables(NodeUtils.getMdsalNode(node), databaseName);
    }
}
