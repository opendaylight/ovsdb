/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Keith Burns
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.compatibility.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.compatibility.plugin.error.OvsdbPluginException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

public class ConfigurationServiceImpl implements OvsdbConfigurationService
{
    org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService pluginOvsdbConfigurationService;

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
        if(this.pluginOvsdbConfigurationService != null)
            this.pluginOvsdbConfigurationService = null;
    }


    /*
     * Though this is a New API that takes in Row object, this still is considered a
     * Deprecated call because of the assumption with a Single Row insertion.
     * An ideal insertRow must be able to take in multiple Rows, which includes the
     * Row being inserted in one Table and other Rows that needs mutate in other Tables.
     */
    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row) {
    	return StatusConvertorUtil.convertOvsdbStatusWithUuidToCompLayerStatusWithUuid(pluginOvsdbConfigurationService.insertRow(node, tableName, parentUuid, row));
    }

    @Override
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Row row) {
    	return StatusConvertorUtil.convertOvsdbStatusToSalStatus(pluginOvsdbConfigurationService.updateRow(node, tableName, parentUUID, rowUUID, row));
    }

    @Override
    public Status deleteRow(Node node, String tableName, String uuid) {
    	return StatusConvertorUtil.convertOvsdbStatusToSalStatus(pluginOvsdbConfigurationService.deleteRow(node, tableName, uuid));
    }

    @Override
    public ConcurrentMap<String, Row> getRows(Node node, String tableName) {
        return pluginOvsdbConfigurationService.getRows(node, tableName);
    }

    @Override
    public Row getRow(Node node, String tableName, String uuid) {
        return pluginOvsdbConfigurationService.getRow(node, tableName, uuid);
    }

    @Override
    public List<String> getTables(Node node) {
        return pluginOvsdbConfigurationService.getTables(node);
    }

    @Override
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException {
    	return pluginOvsdbConfigurationService.setOFController(node, bridgeUUID);
    }

    @Override
    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass) {
    	return pluginOvsdbConfigurationService.getTableName(node, typedClass);
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row) {
    	return pluginOvsdbConfigurationService.getTypedRow(node, typedClass, row);
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass) {
    	return pluginOvsdbConfigurationService.createTypedRow(node, typedClass);
    }

    /**
     * insert a Row in a Table of a specified Database Schema.
     *
     * This method can insert just a single Row specified in the row parameter.
     * But {@link #insertTree(Node, String, String, UUID, Row<GenericTableSchema>) insertTree}
     * can insert a hierarchy of rows with parent-child relationship.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentTable Name of the Parent Table to which this operation will result in attaching/mutating.
     * @param parentUuid UUID of a Row in parent table to which this operation will result in attaching/mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated with the UUID that results from the insert operation.
     * @param row Row of table Content to be inserted
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    @Override
    public UUID insertRow(Node node, String databaseName, String tableName, String parentTable, UUID parentUuid,
                          String parentColumn, Row<GenericTableSchema> row) throws OvsdbPluginException {
    	return pluginOvsdbConfigurationService.insertRow(node, databaseName, tableName, parentTable, parentUuid,
                parentColumn, row);
    }

    /**
     * insert a Row in a Table of a specified Database Schema. This is a convenience method on top of
     * {@link insertRow(Node, String, String, String, UUID, String, Row<GenericTableSchema>) insertRow}
     * which assumes that OVSDB schema implementation that corresponds to the databaseName will provide
     * the necessary service to populate the Parent Table Name and Parent Column Name.
     *
     * This method can insert just a single Row specified in the row parameter.
     * But {@link #insertTree(Node, String, String, UUID, Row<GenericTableSchema>) insertTree}
     * can insert a hierarchy of rows with parent-child relationship.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row of table Content to be inserted
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    @Override
    public UUID insertRow(Node node, String databaseName, String tableName,
            UUID parentRowUuid, Row<GenericTableSchema> row)
            throws OvsdbPluginException {
        return this.insertRow(node, databaseName, tableName, null, parentRowUuid, null, row);
    }

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child relationships referenced through the OVSDB schema's refTable construct
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentTable Name of the Parent Table to which this operation will result in attaching/mutating.
     * @param parentUuid UUID of a Row in parent table to which this operation will result in attaching/mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated with the UUID that results from the insert operation.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row populated in the _uuid column of every row in the tree
     */
    @Override
    public Row<GenericTableSchema> insertTree(Node node, String databaseName, String tableName, String parentTable, UUID parentUuid,
                                              String parentColumn, Row<GenericTableSchema> row) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService.insertTree(node, databaseName, tableName, parentTable, parentUuid, parentColumn, row);
    }

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child relationships referenced through the OVSDB schema's refTable construct.
     * This is a convenience method on top of {@link #insertTree(Node, String, String, String, UUID, String, Row<GenericTableSchema>) insertTree}
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of a Row in parent table to which this operation will result in attaching/mutating.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row populated in the _uuid column of every row in the tree
     */
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
        return pluginOvsdbConfigurationService.updateRow(node, databaseName, tableName, rowUuid, row, overwrite);
    }

    @Override
    public void deleteRow(Node node, String databaseName, String tableName, String parentTable, UUID parentRowUuid,
            String parentColumn, UUID rowUuid) throws OvsdbPluginException {
        pluginOvsdbConfigurationService.deleteRow(node, databaseName, tableName, parentTable, parentRowUuid, parentColumn, rowUuid);
    }

    @Override
    public void deleteRow(Node node, String databaseName, String tableName, UUID rowUuid) throws OvsdbPluginException {
        this.deleteRow(node, databaseName, tableName, null, null, null, rowUuid);
    }

    @Override
    public Row<GenericTableSchema> getRow(Node node, String databaseName,
            String tableName, UUID uuid) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService.getRow(node, databaseName, tableName, uuid);
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node,
            String databaseName, String tableName) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService.getRows(node, databaseName, tableName);
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node,
            String databaseName, String tableName, String fiqlQuery)
            throws OvsdbPluginException {
        return this.getRows(node, databaseName, tableName);
    }

    @Override
    public List<String> getTables(Node node, String databaseName) throws OvsdbPluginException {
        return pluginOvsdbConfigurationService.getTables(node, databaseName);
    }
}
