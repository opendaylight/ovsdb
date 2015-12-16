/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AbstractTransactCommandTest {

    @Mock private HwvtepOperationalState operationalState;
    @Mock private Collection<DataTreeModification<Node>> changes;
    private AbstractTransactCommandChild abstractTransactionCommand;

    public class AbstractTransactCommandChild extends AbstractTransactCommand {
        public AbstractTransactCommandChild(HwvtepOperationalState state,
                        Collection<DataTreeModification<Node>> changes) {
            super(state, changes);
        }

        @Override
        public void execute(TransactionBuilder transaction) {
            // TODO Auto-generated method stub
        }
    }

    @Before
    public void setUp() throws Exception {
        operationalState = mock(HwvtepOperationalState.class, Mockito.RETURNS_MOCKS);
        changes = mock(Collection.class, Mockito.RETURNS_MOCKS);
        abstractTransactionCommand = new AbstractTransactCommandChild(operationalState, changes);
    }

    @Test
    public void testGetOperationalState() {
        assertNotNull(abstractTransactionCommand.getOperationalState());
        assertEquals(operationalState, abstractTransactionCommand.getOperationalState());
    }

    @Test
    public void testGetChanges() {
        assertNotNull(abstractTransactionCommand.getChanges());
        assertEquals(changes, abstractTransactionCommand.getChanges());
    }
}