/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.transactions.md.AbstractTransactionCommand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AbstractTransactionCommandTest {
    private AbstractTransactionCommandChild abstractTransactionCommand1;
    @Mock private OvsdbConnectionInstance key;
    @Mock private TableUpdates updates;
    @Mock private DatabaseSchema dbSchema;

    class AbstractTransactionCommandChild extends AbstractTransactionCommand{
        protected AbstractTransactionCommandChild (OvsdbConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
            super(key, updates, dbSchema);
        }
        @Override
        public void execute(ReadWriteTransaction transaction) {
            // Auto-generated method stub
        }
    }

    @Test
    public void testAbstractTransactionCommand() {
        abstractTransactionCommand1 = new AbstractTransactionCommandChild(key, updates, dbSchema);
        assertEquals(updates, abstractTransactionCommand1.getUpdates());
        assertEquals(dbSchema, abstractTransactionCommand1.getDbSchema());
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        assertEquals(key, abstractTransactionCommand1.getOvsdbConnectionInstance());
        when(key.getMDConnectionInfo()).thenReturn(connectionInfo);
        assertEquals(connectionInfo, abstractTransactionCommand1.getConnectionInfo());
    }
}
