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
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AbstractTransactCommandTest {

    @Mock private BridgeOperationalState operationalState;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;

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

    @Test
    public void testAbstractTransactCommand() {
        AbstractTransactCommandChild abstractTransCmd = new AbstractTransactCommandChild(operationalState, changes);
        assertEquals(operationalState, abstractTransCmd.getOperationalState());
        assertEquals(changes, abstractTransCmd.getChanges());
    }
}