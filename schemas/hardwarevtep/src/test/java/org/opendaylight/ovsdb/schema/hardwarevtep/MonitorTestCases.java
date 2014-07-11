/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Madhu Venugopal, Dave Tucker, Matt Oswalt
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class MonitorTestCases extends HardwareVtepSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(MonitorTestCases.class);

    @Override
    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void monitorTables() throws ExecutionException, InterruptedException, IOException, TimeoutException {
        //Ensure test only proceeds if HW VTEP is supported
        Assume.assumeTrue(super.checkSchema());
        HardwareVtepSchemaSuiteIT.dbSchema = this.ovs.getSchema(HARDWARE_VTEP_SCHEMA).get();
        Assert.assertNotNull(HardwareVtepSchemaSuiteIT.dbSchema);

        List<MonitorRequest<GenericTableSchema>> monitorRequests = Lists.newArrayList();
        monitorRequests.add(this.getAllColumnsMonitorRequest(Manager.class));
        monitorRequests.add(this.getAllColumnsMonitorRequest(Global.class));

        TableUpdates updates = ovs.monitor(HardwareVtepSchemaSuiteIT.dbSchema, monitorRequests, new UpdateMonitor());
        Assert.assertNotNull(updates);
        this.updateTableCache(updates);
    }

    /**
     * As per RFC 7047, section 4.1.5, if a Monitor request is sent without any columns, the update response will not include
     * the _uuid column.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * Each <monitor-request> specifies one or more columns and the manner in which the columns (or the entire table) are to be monitored.
     * The "columns" member specifies the columns whose values are monitored. It MUST NOT contain duplicates.
     * If "columns" is omitted, all columns in the table, except for "_uuid", are monitored.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * In order to overcome this limitation, this method
     *
     * @return MonitorRequest that includes all the Bridge Columns including _uuid
     */
    public <T extends TypedBaseTable<GenericTableSchema>> MonitorRequest<GenericTableSchema> getAllColumnsMonitorRequest (Class <T> klazz) { //TODO: Needs updated for HW VTEP
        TypedBaseTable<GenericTableSchema> table = ovs.createTypedRowWrapper(klazz);
        GenericTableSchema bridgeSchema = table.getSchema();
        Set<String> columns = bridgeSchema.getColumns();
        MonitorRequestBuilder<GenericTableSchema> bridgeBuilder = MonitorRequestBuilder.builder(table.getSchema());
        for (String column : columns) {
            bridgeBuilder.addColumn(column);
        }
        return bridgeBuilder.with(new MonitorSelect(true, true, true, true)).build();
    }



    @Override
    public void update(Object context, UpdateNotification upadateNotification) {

    }

    @Override
    public void locked(Object context, List<String> ids) {

    }

    @Override
    public void stolen(Object context, List<String> ids) {

    }

    private void updateTableCache(TableUpdates updates) {
        for (String tableName : updates.getUpdates().keySet()) {
            Map<UUID, Row> tUpdate = HardwareVtepSchemaSuiteIT.getTableCache().get(tableName);
            TableUpdate update = updates.getUpdates().get(tableName);
            for (UUID uuid : (Set<UUID>)update.getRows().keySet()) {
                if (update.getNew(uuid) != null) {
                    if (tUpdate == null) {
                        tUpdate = new HashMap<>();
                        HardwareVtepSchemaSuiteIT.getTableCache().put(tableName, tUpdate);
                    }
                    tUpdate.put(uuid, update.getNew(uuid));
                } else {
                    tUpdate.remove(uuid);
                }
            }
        }
    }

    private class UpdateMonitor implements MonitorCallBack {
        @Override
        public void update(TableUpdates result, DatabaseSchema dbSchema) {
            updateTableCache(result);
        }

        @Override
        public void exception(Throwable t) {
            System.out.println("Exception t = " + t);
        }
    }
}
