/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class TypedHardwareVTepSchemaIT extends OvsdbTestBase {

    Logger logger = LoggerFactory.getLogger(TypedHardwareVTepSchemaIT.class);
    OvsdbClient ovs;
    DatabaseSchema dbSchema = null;

    @Test
    public void testGetDBs() throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = ovs.getDatabases();
        List<String> dbNames = databases.get();
        Assert.assertNotNull(dbNames);
        boolean hasHardwareVTepSchema = false;
        for(String dbName : dbNames) {
           if (dbName.equals(HARDWARE_VTEP_SCHEMA)) {
                hasHardwareVTepSchema = true;
                break;
           }
        }
        Assume.assumeTrue(hasHardwareVTepSchema);
    }

    @Before
    public  void setUp() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (ovs != null) {
            return;
        }
        ovs = getTestConnection();
        testGetDBs();
        dbSchema = ovs.getSchema(HARDWARE_VTEP_SCHEMA, true).get();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
    }

    @Override
    public void update(Object node, UpdateNotification upadateNotification) {
        // TODO Auto-generated method stub

    }
    @Override
    public void locked(Object node, List<String> ids) {
        // TODO Auto-generated method stub

    }
    @Override
    public void stolen(Object node, List<String> ids) {
        // TODO Auto-generated method stub

    }
}
