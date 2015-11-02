/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest(BridgeRemovedCommand.class)
@RunWith(PowerMockRunner.class)
public class BridgeRemovedCommandTest {

    @Mock private BridgeRemovedCommand briRemovedCmd;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> returnChanges;
    @Mock private Set<InstanceIdentifier<OvsdbBridgeAugmentation>> removed;
    @Mock private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> originals;

    @Before
    public void setUp() throws Exception {
        briRemovedCmd = mock(BridgeRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testExecute() throws Exception {
        TransactionBuilder transaction = mock( TransactionBuilder.class, Mockito.RETURNS_MOCKS);

        PowerMockito.mockStatic(TransactUtils.class);
        when(TransactUtils.extractRemoved(changes, OvsdbBridgeAugmentation.class)).thenReturn(removed);
        when(TransactUtils.extractOriginal(changes, OvsdbBridgeAugmentation.class)).thenReturn(originals);

        MemberModifier.suppress(MemberMatcher.method(BridgeRemovedCommand.class, "getChanges"));
        when(briRemovedCmd.getChanges()).thenReturn(returnChanges);

        briRemovedCmd.execute(transaction);
        verify(briRemovedCmd, times(2)).getChanges();
    }

}
