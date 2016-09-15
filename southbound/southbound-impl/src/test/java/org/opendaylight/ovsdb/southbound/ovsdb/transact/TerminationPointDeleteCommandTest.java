/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TransactUtils.class, TerminationPointDeleteCommand.class, TyperUtils.class})
public class TerminationPointDeleteCommandTest {

    private TerminationPointDeleteCommand terminationPointDeleteCommand;
    @Mock private BridgeOperationalState state;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    private Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
        originals = new HashMap<>();
    private Map<InstanceIdentifier<Node>, Node> originalNodes = new HashMap<>();
    private Set<InstanceIdentifier<OvsdbTerminationPointAugmentation>> removedTps = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        terminationPointDeleteCommand = mock(TerminationPointDeleteCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testExecute() {
        PowerMockito.mockStatic(TransactUtils.class);
        when(TransactUtils.extractOriginal(changes, OvsdbTerminationPointAugmentation.class)).thenReturn(originals);
        when(TransactUtils.extractOriginal(changes, Node.class)).thenReturn(originalNodes);
        when(TransactUtils.extractRemoved(changes, OvsdbTerminationPointAugmentation.class)).thenReturn(removedTps);
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        terminationPointDeleteCommand.execute(transaction, state, changes, mock(InstanceIdentifierCodec.class));

        // TODO Actually verify something
    }
}
