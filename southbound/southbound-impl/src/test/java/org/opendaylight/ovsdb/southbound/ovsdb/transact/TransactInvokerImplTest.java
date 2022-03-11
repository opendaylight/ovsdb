/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({TransactInvokerImpl.class})
@RunWith(PowerMockRunner.class)
public class TransactInvokerImplTest {

    @Mock private OvsdbConnectionInstance connectionInstance;
    @Mock private DatabaseSchema dbSchema;
    private TransactInvokerImpl transactInvokerImpl;

    @Before
    public void setUp() throws Exception {
        transactInvokerImpl = new TransactInvokerImpl(connectionInstance, dbSchema);
        MemberModifier.field(TransactInvokerImpl.class, "connectionInstance").set(transactInvokerImpl,
                connectionInstance);
        MemberModifier.field(TransactInvokerImpl.class, "dbSchema").set(transactInvokerImpl, dbSchema);
    }

    @Test
    public void testTransactionInvokerImpl() {
        TransactInvokerImpl transactInvokerImpl1 = new TransactInvokerImpl(connectionInstance, dbSchema);
        assertEquals(connectionInstance, Whitebox.getInternalState(transactInvokerImpl1, "connectionInstance"));
        assertEquals(dbSchema, Whitebox.getInternalState(transactInvokerImpl1, "dbSchema"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testInvoke() throws Exception {
        TransactCommand command = mock(TransactCommand.class);
        TransactionBuilder tb = mock(TransactionBuilder.class);
        PowerMockito.whenNew(TransactionBuilder.class).withAnyArguments().thenReturn(tb);
        doNothing().when(command).execute(any(TransactionBuilder.class), any(BridgeOperationalState.class),
                any(DataChangeEvent.class), any(InstanceIdentifierCodec.class));

        ListenableFuture<List<OperationResult>> result = mock(ListenableFuture.class);
        when(tb.execute()).thenReturn(result);
        List<Operation> operation = new ArrayList<>();
        operation.add(mock(Operation.class));
        when(tb.getOperations()).thenReturn(operation);
        List<OperationResult> got = new ArrayList<>();
        when(result.get()).thenReturn(got);
        transactInvokerImpl.invoke(command, mock(BridgeOperationalState.class), mock(DataChangeEvent.class),
                mock(InstanceIdentifierCodec.class));
        verify(result).get();
    }
}
