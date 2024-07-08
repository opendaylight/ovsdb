/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.lib.operations.DefaultOperations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;

@RunWith(MockitoJUnitRunner.class)
public class TransactCommandAggregatorTest {

    private final List<TransactCommand> commands = new ArrayList<>();
    private TransactCommandAggregator transactCommandAggregator;
    @Mock private DataChangeEvent changes;
    @Mock private BridgeOperationalState operationalState;

    @Before
    public void setUp() throws Exception {
        transactCommandAggregator = new TransactCommandAggregator(new DefaultOperations());

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
        commands.add(mock(AutoAttachUpdateCommand.class));
        commands.add(mock(AutoAttachRemovedCommand.class));
        commands.add(mock(QosUpdateCommand.class));
        commands.add(mock(QosRemovedCommand.class));
        commands.add(mock(QueueUpdateCommand.class));
        commands.add(mock(QueueRemovedCommand.class));
        commands.add(mock(TerminationPointUpdateCommand.class));
    }

    @Test
    @Ignore("This needs to be rewritten")
    public void testExecute() {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        for (TransactCommand command: commands) {
            doNothing().when(command).execute(any(TransactionBuilder.class), any(BridgeOperationalState.class),
                    any(DataChangeEvent.class), any(InstanceIdentifierCodec.class));
        }
        transactCommandAggregator.execute(transaction, mock(BridgeOperationalState.class),
                mock(DataChangeEvent.class), mock(InstanceIdentifierCodec.class));
        for (TransactCommand command: commands) {
            verify(command).execute(any(TransactionBuilder.class), any(BridgeOperationalState.class),
                    any(DataChangeEvent.class), any(InstanceIdentifierCodec.class));
        }
    }
}
