/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@PrepareForTest()
@Ignore
public class ProtocolRemovedCommandTest {
    private ProtocolRemovedCommand protocolRemovedCommand;

    @Before
    public  void setUpBeforeClass() throws Exception {
        protocolRemovedCommand = mock(ProtocolRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testExecute() throws Exception{
        TransactionBuilder transaction = mock(TransactionBuilder.class);

        Bridge bridge = mock(Bridge.class);
        when(transaction.getDatabaseSchema()).thenReturn(mock(DatabaseSchema.class));
        PowerMockito.mockStatic(TyperUtils.class);
        PowerMockito.when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class),eq(Bridge.class))).thenReturn(bridge);

        when(transaction.add(any(Operation.class))).thenReturn(transaction);

        protocolRemovedCommand.execute(transaction);
        verify(transaction).add(any(Operation.class));
    }
}
