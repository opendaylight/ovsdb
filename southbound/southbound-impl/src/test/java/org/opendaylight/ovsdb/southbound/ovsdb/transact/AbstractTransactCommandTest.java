/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AbstractTransactCommandTest {

    @Mock private BridgeOperationalState operationalState;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    private AbstractTransactCommandChild abstractTransCmd;

    public class AbstractTransactCommandChild extends AbstractTransactCommand {
        public AbstractTransactCommandChild(BridgeOperationalState state, AsyncDataChangeEvent<InstanceIdentifier<?>,
                DataObject> changes) {
            super(state, changes);
        }

        @Override
        public void execute(TransactionBuilder transaction) {
            // TODO Auto-generated method stub
        }
    }

    @Before
    public void setUp() throws Exception {
        operationalState = mock(BridgeOperationalState.class, Mockito.RETURNS_MOCKS);
        changes = mock(AsyncDataChangeEvent.class, Mockito.RETURNS_MOCKS);
        abstractTransCmd = new AbstractTransactCommandChild(operationalState, changes);
    }

    @Test
    public void testGetOperationalState() {
        assertNotNull(abstractTransCmd.getOperationalState());
        assertEquals(operationalState, abstractTransCmd.getOperationalState());
    }

    @Test
    public void testGetChanges() {
        assertNotNull(abstractTransCmd.getChanges());
        assertEquals(changes, abstractTransCmd.getChanges());
    }
}