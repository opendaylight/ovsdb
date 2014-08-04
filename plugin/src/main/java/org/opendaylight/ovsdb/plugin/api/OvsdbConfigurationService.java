/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin.api;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.plugin.error.OvsdbPluginException;

public interface OvsdbConfigurationService {

    /**
     * @deprecated This version of insertRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * This API is replaced by
     * {@link #insertRow(Node, String, String, String, Row<GenericTableSchema>) insertRow} and
     * {@link #insertTree(Node, String, String, String, Row<GenericTableSchema>) insertTree}
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row of table Content to be inserted
     * @return UUID of the inserted Row
     */
    @Deprecated
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row);

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
     * @param parentUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row of table Content to be inserted
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    public UUID insertRow(Node node, String databaseName, String tableName, UUID parentUuid, Row<GenericTableSchema> row) throws OvsdbPluginException;

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child relationships referenced through the OVSDB schema's refTable construct
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row populated in the _uuid column of every row in the tree
     */
    public Row<GenericTableSchema> insertTree(Node node, String databaseName, String tableName, UUID parentUuid, Row<GenericTableSchema> row) throws OvsdbPluginException;

    /**
     * @deprecated This version of updateRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * This API is replaced by
     * {@link #updateRow(Node, String, String, UUID, Row<GenericTableSchema>, boolean) updateRow}
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is Updated
     * @param parentUuid UUID of the parent row on which this operation might result in mutating.
     * @param rowUuid UUID of the row that is being updated
     * @param row Row of table Content to be Updated. Include just those columns that needs to be updated.
     */
    @Deprecated
    public Status updateRow (Node node, String tableName, String parentUuid, String rowUuid, Row row);

    /**
     * update or mutate a Row in a Table of a specified Database Schema.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is updated
     * @param rowUuid UUID of the row being updated
     * @param row Row of table Content to be updated
     * @param overwrite true will overwrite/replace the existing row (matching the rowUuid) with the passed row object.
     *                  false will update the existing row (matching the rowUuid) using only the columns in the passed row object.
     * @throws OvsdbPluginException Any failure during the update operation will result in a specific exception.
     * @return Returns the entire Row after the update operation.
     */
    public Row<GenericTableSchema> updateRow(Node node, String databaseName, String tableName, UUID rowUuid,
                            Row<GenericTableSchema> row, boolean overwrite) throws OvsdbPluginException;

    /**
     * @deprecated This version of deleteRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * This API is replaced by {@link #deleteRow(Node, String, String, UUID) deleteRow}
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is Updated
     * @param rowUuid UUID of the row that is being deleted
     */
    @Deprecated
    public Status deleteRow (Node node, String tableName, String rowUuid);

    /**
     * update or mutate a Row in a Table of a specified Database Schema.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is Updated
     * @param rowUuid UUID of the row that is being deleted
     * @throws OvsdbPluginException Any failure during the delete operation will result in a specific exception.
     */

    public void deleteRow (Node node, String databaseName, String tableName, UUID rowUuid) throws OvsdbPluginException;

    /**
     * @deprecated This version of getRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * This API is replaced by {@link #getRow(Node, String, String, UUID) getRow}
     *
     * @param node OVSDB Node
     * @param tableName Table Name
     * @param rowUuid UUID of the row being queried
     * @return a row with a list of Column data that corresponds to an unique Row-identifier called uuid in a given table.
     */
    @Deprecated
    public Row getRow(Node node, String tableName, String uuid);

    /**
     * Returns a Row from a table for the specified uuid.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table Name
     * @param uuid UUID of the row being queried
     * @throws OvsdbPluginException Any failure during the get operation will result in a specific exception.
     * @return a row with a list of Column data that corresponds to an unique Row-identifier called uuid in a given table.
     */
    public Row<GenericTableSchema> getRow(Node node, String databaseName, String tableName, UUID uuid) throws OvsdbPluginException;

    /**
     * @Deprecated This version of getRows is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * This API is replaced by
     * {@link #getRows(Node, String, String) getRows} and {@link #getRows(Node, String, String, String) getRows}
     *
     * @param node OVSDB Node
     * @param tableName Table Name
     * @return List of rows that makes the entire Table.
     */
    @Deprecated
    public ConcurrentMap<String, Row> getRows(Node node, String tableName);

    /**
     * Returns all rows of a table.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table Name
     * @throws OvsdbPluginException Any failure during the get operation will result in a specific exception.
     * @return Map of rows to its UUID that makes the entire Table.
     */
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node, String databaseName, String tableName) throws OvsdbPluginException;

    /**
     * Returns all rows of a table filtered by query string.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table Name
     * @param fiqlQuery FIQL style String Query {@link http://tools.ietf.org/html/draft-nottingham-atompub-fiql-00} to filter rows
     * @throws OvsdbPluginException Any failure during the get operation will result in a specific exception.
     * @return Map of rows to its UUID that makes the entire Table.
     */
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node, String databaseName, String tableName, String fiqlQuery) throws OvsdbPluginException;

    /**
     * @Deprecated Returns all the Tables in a given Ndoe.
     * This API assumes an Open_vSwitch database Schema.
     *
     * This API is replaced by
     * {@link #getTables(Node, String) getTables}
     * @param node OVSDB node
     * @return List of Table Names that make up Open_vSwitch schema.
     */
    @Deprecated
    public List<String> getTables(Node node);

    /**
     * Returns all the Tables in a given Node.
     *
     * @param node OVSDB node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @throws OvsdbPluginException Any failure during the get operation will result in a specific exception.
     * @return List of Table Names that make up the schema represented by the databaseName
     */
    public List<String> getTables(Node node, String databaseName) throws OvsdbPluginException;

    /**
     * setOFController is a convenience method used by existing applications to setup Openflow Controller on
     * a Open_vSwitch Bridge.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node Node
     * @param bridgeUUID uuid of the Bridge for which the ip-address of Openflow Controller should be programmed.
     * @return Boolean representing success or failure of the operation.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException;

    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass);
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row);
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass);
}
