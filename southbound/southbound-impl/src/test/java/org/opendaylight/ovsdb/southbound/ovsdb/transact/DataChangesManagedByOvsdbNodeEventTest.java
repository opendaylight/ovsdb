/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertEquals;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest()
public class DataChangesManagedByOvsdbNodeEventTest {

    @Mock private InstanceIdentifier<?> iid;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private Set<InstanceIdentifier<?>> removedPaths;
    private DataChangesManagedByOvsdbNodeEvent dataChangesManagedByOvsdbNodeEvent;

    @Before
    public void setUp() throws Exception {
        dataChangesManagedByOvsdbNodeEvent = mock(DataChangesManagedByOvsdbNodeEvent.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(DataChangesManagedByOvsdbNodeEvent.class, "event").set(dataChangesManagedByOvsdbNodeEvent, event);
    }

    @Test
    public void testDataChangesManagedByOvsdbNodeEvent() {
        DataChangesManagedByOvsdbNodeEvent dataChangesManagedByOvsdbNodeEvent1 = new DataChangesManagedByOvsdbNodeEvent(iid, event);
        assertEquals(iid, Whitebox.getInternalState(dataChangesManagedByOvsdbNodeEvent1, "iid"));
        assertEquals(event, Whitebox.getInternalState(dataChangesManagedByOvsdbNodeEvent1, "event"));
    }

    @Test
    public void testGetMethods() {
        Map<InstanceIdentifier<?>,DataObject> data = new HashMap<>();
        DataObject dataObject = mock(DataObject.class);

        //Test getCreatedData()
        when(event.getCreatedData()).thenReturn(data);
        assertEquals(data, dataChangesManagedByOvsdbNodeEvent.getCreatedData());

        //Test getUpdatedData()
        when(event.getUpdatedData()).thenReturn(data);
        assertEquals(data, dataChangesManagedByOvsdbNodeEvent.getUpdatedData());

        //Test getOriginalData()
        when(event.getOriginalData()).thenReturn(data);
        assertEquals(data, dataChangesManagedByOvsdbNodeEvent.getOriginalData());

        //Test getOriginalSubtree()
        when(event.getOriginalSubtree()).thenReturn(dataObject);
        assertEquals(dataObject, dataChangesManagedByOvsdbNodeEvent.getOriginalSubtree());

        //Test getUpdatedSubtree()
        when(event.getUpdatedSubtree()).thenReturn(dataObject);
        assertEquals(dataObject, dataChangesManagedByOvsdbNodeEvent.getUpdatedSubtree());

        //Test getRemovedPaths()
        removedPaths = new HashSet<>();
        when(event.getRemovedPaths()).thenReturn(removedPaths);
        assertEquals(removedPaths, dataChangesManagedByOvsdbNodeEvent.getRemovedPaths());
    }
}
