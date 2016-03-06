/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest(ControllerRemovedCommand.class)
@RunWith(PowerMockRunner.class)
public class ControllerRemovedCommandTest {

    @Mock private ControllerRemovedCommand contRemoveCmd;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> returnChanges;
    @Mock private Set<InstanceIdentifier<ControllerEntry>> removed;
    @Mock private Map<InstanceIdentifier<ControllerEntry>, ControllerEntry> operationalControllerEntries;
    @Mock private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> created;
    @Mock private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updated;
    @Before
    public void setUp() throws Exception {
        contRemoveCmd = mock(ControllerRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testExecute() throws Exception {
        TransactionBuilder transaction = mock( TransactionBuilder.class, Mockito.RETURNS_MOCKS);

        PowerMockito.mockStatic(TransactUtils.class);
        when(TransactUtils.extractRemoved(changes, ControllerEntry.class)).thenReturn(removed);
        when(TransactUtils.extractOriginal(changes, ControllerEntry.class)).thenReturn(operationalControllerEntries);
        when(TransactUtils.extractCreated(changes, OvsdbBridgeAugmentation.class)).thenReturn(created);
        when(TransactUtils.extractUpdated(changes, OvsdbBridgeAugmentation.class)).thenReturn(updated);

        MemberModifier.suppress(MemberMatcher.method(ControllerRemovedCommand.class, "getChanges"));
        when(contRemoveCmd.getChanges()).thenReturn(returnChanges);

        contRemoveCmd.execute(transaction);
        verify(contRemoveCmd, times(3)).getChanges();
    }

}
