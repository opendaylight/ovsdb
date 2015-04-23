/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * Unit test for {@link OvsdbBridgeRemovedCommand}
 */
@PrepareForTest({TyperUtils.class, SouthboundMapper.class})
@RunWith(PowerMockRunner.class)
public class OvsdbBridgeUpdateCommandTest {

    @InjectMocks private OvsdbBridgeUpdateCommand ovsdbBridgeUpdateCommand = new OvsdbBridgeUpdateCommand();

    @Test
    public void test() {
        PowerMockito.mockStatic(TyperUtils.class);
        Map<UUID, Controller> map = new HashMap();
        Mockito.when(TyperUtils.extractRowsUpdated(Mockito.same(Controller.class), Mockito.any(TableUpdates.class), Mockito.any(DatabaseSchema.class))).thenReturn(map );

        PowerMockito.mockStatic(SouthboundMapper.class);
        List<ControllerEntry> list = new ArrayList();
        ControllerEntry controllerEntry = Mockito.mock(ControllerEntry.class);
        list.add(controllerEntry);
        Mockito.when(controllerEntry.getTarget()).thenReturn(new Uri("tc[:192.168.1.107:6640"));
        Mockito.when(controllerEntry.isIsConnected()).thenReturn(true);
        Mockito.when(SouthboundMapper.createControllerEntries(Mockito.any(Bridge.class), Mockito.any(Map.class))).thenReturn(list );

//        ovsdbBridgeUpdateCommand.setOpenFlowNodeRef(Mockito.mock(OvsdbBridgeAugmentationBuilder.class), Mockito.mock(Bridge.class));
    }

}
