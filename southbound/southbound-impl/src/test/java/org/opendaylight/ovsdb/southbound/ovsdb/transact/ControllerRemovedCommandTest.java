/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest(ControllerRemovedCommand.class)
@RunWith(PowerMockRunner.class)
public class ControllerRemovedCommandTest {

    private ControllerRemovedCommand contRemoveCmd = new ControllerRemovedCommand();
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> returnChanges;
    private Set<InstanceIdentifier<ControllerEntry>> removed = new HashSet<>();
    private Map<InstanceIdentifier<ControllerEntry>, ControllerEntry> operationalControllerEntries = new HashMap<>();
    private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> created = new HashMap<>();
    private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updated = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        contRemoveCmd = mock(ControllerRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testExecute() throws Exception {
        PowerMockito.mockStatic(TransactUtils.class);
        when(TransactUtils.extractRemoved(changes, ControllerEntry.class)).thenReturn(removed);
        when(TransactUtils.extractOriginal(changes, ControllerEntry.class)).thenReturn(operationalControllerEntries);
        when(TransactUtils.extractCreated(changes, OvsdbBridgeAugmentation.class)).thenReturn(created);
        when(TransactUtils.extractUpdated(changes, OvsdbBridgeAugmentation.class)).thenReturn(updated);

        TransactionBuilder transaction = mock(TransactionBuilder.class, Mockito.RETURNS_MOCKS);
        contRemoveCmd.execute(transaction, null, changes, mock(InstanceIdentifierCodec.class));

        // TODO Actually verify something
    }

}
