/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.table.Table;

import com.fasterxml.jackson.core.JsonParseException;

public interface OVSDBConfigService {
    public StatusWithUuid insertRow (Node node, String tableName, String parentUUID, Table<?> row);
    public Status deleteRow (Node node, String tableName, String rowUUID);
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Table<?> row);
    public String getSerializedRow(Node node, String tableName, String uuid) throws Exception;
    public String getSerializedRows(Node node, String tableName) throws Exception;
    public Table<?> getRow(Node node, String tableName, String uuid) throws Exception;
    public ConcurrentMap<String, Table<?>> getRows(Node node, String tableName) throws Exception;
    public List<String> getTables(Node node) throws Exception;


    /**
     * This version of insertRow is a short-term replacement for the older & now deprecated version.
     * This API assumes an Open_vSwitch database Schema.
     *
     * @param node OVSDB Node
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row of table Content to be inserted
     * @return UUID of the inserted Row
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws JsonParseException
     * @throws IOException
     */
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid,
                                    Row<GenericTableSchema> row) throws InterruptedException, ExecutionException, JsonParseException, IOException;
}
