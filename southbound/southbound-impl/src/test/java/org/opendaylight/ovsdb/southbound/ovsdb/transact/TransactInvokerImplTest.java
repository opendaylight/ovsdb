/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TransactInvokerImplTest {
    @Mock
    private OvsdbConnectionInstance connectionInstance;
    @Mock
    private DatabaseSchema dbSchema;
    private TransactInvokerImpl transactInvokerImpl;

    @Before
    public void setUp() throws Exception {
        transactInvokerImpl = new TransactInvokerImpl(connectionInstance, dbSchema);
    }

    @Test
    public void testTransactionInvokerImpl() {
        assertSame(connectionInstance, Whitebox.getInternalState(transactInvokerImpl, "connectionInstance"));
        assertSame(dbSchema, Whitebox.getInternalState(transactInvokerImpl, "dbSchema"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception {
        final ListenableFuture<List<OperationResult>> result = mock(ListenableFuture.class);
        doReturn(List.of()).when(result).get();

        try (var mocked = mockConstruction(TransactionBuilder.class, withSettings(), (mock, context) -> {
            doReturn(result).when(mock).execute();
            doReturn(List.of(mock(Operation.class))).when(mock).getOperations();
        })) {
            TransactCommand command = mock(TransactCommand.class);
            doNothing().when(command).execute(any(TransactionBuilder.class), any(BridgeOperationalState.class),
                    any(DataChangeEvent.class), any(InstanceIdentifierCodec.class));

            transactInvokerImpl.invoke(command, mock(BridgeOperationalState.class), mock(DataChangeEvent.class),
                    mock(InstanceIdentifierCodec.class));
            verify(result).get();
        }
    }
}
