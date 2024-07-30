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
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class DataChangesManagedByOvsdbNodeEventTest {
    private final InstanceIdentifier<?> iid = InstanceIdentifier.create(NetworkTopology.class);

    @Mock private DataBroker db;
    @Mock private DataChangeEvent event;
    private Set<InstanceIdentifier<?>> removedPaths;
    private DataChangesManagedByOvsdbNodeEvent dataChangesManagedByOvsdbNodeEvent;

    @Before
    public void setUp() throws Exception {
        dataChangesManagedByOvsdbNodeEvent = mock(DataChangesManagedByOvsdbNodeEvent.class, Mockito.CALLS_REAL_METHODS);
        Whitebox.getField(DataChangesManagedByOvsdbNodeEvent.class, "event").set(dataChangesManagedByOvsdbNodeEvent,
            event);
    }

    @Test
    public void testDataChangesManagedByOvsdbNodeEvent() {
        DataChangesManagedByOvsdbNodeEvent dataChangesManagedByOvsdbNodeEvent1 = new
                DataChangesManagedByOvsdbNodeEvent(db, iid, event);
        assertEquals(iid, Whitebox.getInternalState(dataChangesManagedByOvsdbNodeEvent1, "iid"));
        assertEquals(event, Whitebox.getInternalState(dataChangesManagedByOvsdbNodeEvent1, "event"));
    }

    @Test
    public void testGetMethods() {
        Map<InstanceIdentifier<?>,DataObject> data = new HashMap<>();

        //Test getCreatedData()
        when(event.getCreatedData()).thenReturn(data);
        assertEquals(data, dataChangesManagedByOvsdbNodeEvent.getCreatedData());

        //Test getUpdatedData()
        when(event.getUpdatedData()).thenReturn(data);
        assertEquals(data, dataChangesManagedByOvsdbNodeEvent.getUpdatedData());

        //Test getOriginalData()
        when(event.getOriginalData()).thenReturn(data);
        assertEquals(data, dataChangesManagedByOvsdbNodeEvent.getOriginalData());

        DataObject dataObject = mock(DataObject.class);

        //Test getRemovedPaths()
        removedPaths = new HashSet<>();
        when(event.getRemovedPaths()).thenReturn(removedPaths);
        assertEquals(removedPaths, dataChangesManagedByOvsdbNodeEvent.getRemovedPaths());
    }
}
