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
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.plugin.StatusWithUuid;

public interface OvsdbConfigurationService {

    /**
     * This version of insertRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row of table Content to be inserted
     * @return UUID of the inserted Row
     */
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row);

    /**
     * This version of updateRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is Updated
     * @param parentUuid UUID of the parent row on which this operation might result in mutating.
     * @param rowUuid UUID of the row that is being updated
     * @param row Row of table Content to be Updated. Include just those columns that needs to be updated.
     */
    public Status updateRow (Node node, String tableName, String parentUuid, String rowUuid, Row row);

    /**
     * This version of deleteRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is Updated
     * @param rowUuid UUID of the row that is being deleted
     */

    public Status deleteRow (Node node, String tableName, String rowUUID);

    /**
     * This version of getRow is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB Node
     * @param tableName Table Name
     * @param rowUuid UUID of the row being queried
     * @return a row with a list of Column data that corresponds to an unique Row-identifier called uuid in a given table.
     */

    public Row getRow(Node node, String tableName, String uuid);

    /**
     * This version of getRows is a short-term replacement for the older & now deprecated method of the same name.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB Node
     * @param tableName Table Name
     * @return List of rows that makes the entire Table.
     */

    public ConcurrentMap<String, Row> getRows(Node node, String tableName);

    /**
     * Returns all the Tables in a given Ndoe.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB node
     * @return List of Table Names that make up Open_vSwitch schema.
     */
    public List<String> getTables(Node node);

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
