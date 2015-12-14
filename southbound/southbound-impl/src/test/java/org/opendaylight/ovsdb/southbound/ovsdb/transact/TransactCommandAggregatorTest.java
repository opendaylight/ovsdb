/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({})
@RunWith(PowerMockRunner.class)
public class TransactCommandAggregatorTest {
    private static final int NUMBER_OF_COMMANDS = 15;
    private List<TransactCommand> commands = new ArrayList<>();
    private TransactCommandAggregator transactCommandAggregator;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    @Mock private BridgeOperationalState operationalState;

    @Before
    public void setUp() throws Exception {
        transactCommandAggregator = PowerMockito.mock(TransactCommandAggregator.class, Mockito.CALLS_REAL_METHODS);

        //mock commands field
        commands.add(mock(BridgeUpdateCommand.class));
        commands.add(mock(OpenVSwitchBridgeAddCommand.class));
        commands.add(mock(ControllerUpdateCommand.class));
        commands.add(mock(ControllerRemovedCommand.class));
        commands.add(mock(ProtocolUpdateCommand.class));
        commands.add(mock(ProtocolRemovedCommand.class));
        commands.add(mock(BridgeRemovedCommand.class));
        commands.add(mock(TerminationPointCreateCommand.class));
        commands.add(mock(TerminationPointDeleteCommand.class));
        commands.add(mock(OvsdbNodeUpdateCommand.class));
        commands.add(mock(QosUpdateCommand.class));
        commands.add(mock(QosRemovedCommand.class));
        commands.add(mock(QueueUpdateCommand.class));
        commands.add(mock(QueueRemovedCommand.class));
        commands.add(mock(TerminationPointUpdateCommand.class));
        MemberModifier.field(TransactCommandAggregator.class, "commands").set(transactCommandAggregator, commands);
    }

    @Test
    public void testOvsdbOperationalCommandAggregator() throws Exception {
        TransactCommandAggregator transactCommandAggregator1 = new TransactCommandAggregator(operationalState, changes);
        List<TransactCommand> testCommands = Whitebox.getInternalState(transactCommandAggregator1, "commands");
        assertEquals(NUMBER_OF_COMMANDS, testCommands.size());
    }

    @Test
    public void testExecute() {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        for (TransactCommand command: commands) {
            doNothing().when(command).execute(any(TransactionBuilder.class));
        }
        transactCommandAggregator.execute(transaction);
        for (TransactCommand command: commands) {
            verify(command).execute(any(TransactionBuilder.class));
        }
    }
}
