package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

@RunWith(MockitoJUnitRunner.class)
public class VlanConfigurationCacheImplTest {

    @Mock private OvsdbConfigurationService ovsdbConfigurationService;

    @InjectMocks public VlanConfigurationCacheImpl vlanConfigurationCacheImpl;
    @InjectMocks private TenantNetworkManagerImpl tenantNetworkManagerImpl = mock(TenantNetworkManagerImpl.class);

    private static final String NODE_UUID = "nodeUUID";
    private static final String NETWORK_ID= "networkId";

    public void setNodeConfiguration(){
        Row row = mock(Row.class);
        Port port = mock(Port.class);

        ConcurrentHashMap<String, Row> ovsTable;
        ovsTable = new ConcurrentHashMap<>();
        ovsTable.put(NODE_UUID, row);

        ConcurrentHashMap<String, Row> portRows = ovsTable;

        Set<Long> tags = new HashSet<Long>();
        tags.add(new Random().nextLong());

        UUID uuid = mock(UUID.class);
        Set<UUID> uuidSet = new HashSet<>();
        uuidSet.add(uuid);

        Column<GenericTableSchema, Set<Long>> longColumnMock = mock(Column.class);
        Column<GenericTableSchema, Set<UUID>> uuidColumnMock = mock(Column.class);

        Interface iface = mock(Interface.class);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
//
        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(tenantNetworkManagerImpl);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(ovsTable);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(portRows);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Port.class), any(Row.class))).thenReturn(port);


        when(port.getTagColumn()).thenReturn(longColumnMock);
        when(longColumnMock.getData()).thenReturn(tags);
        when(port.getInterfacesColumn()).thenReturn(uuidColumnMock);
        when(uuidColumnMock.getData()).thenReturn(uuidSet);

        when(ovsdbConfigurationService.getRow(any(Node.class), anyString(), anyString())).thenReturn(row);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Interface.class), any(Row.class))).thenReturn(iface);

        when(tenantNetworkManagerImpl.getTenantNetwork(any(Interface.class))).thenReturn(neutronNetwork);
        when(neutronNetwork.getNetworkUUID()).thenReturn("networkUUID");
    }

    @Test
    public void testAssignInternalVlan() {
        setNodeConfiguration();

        assertEquals(1, (int) vlanConfigurationCacheImpl.assignInternalVlan(mock(Node.class), NETWORK_ID));
        assertEquals(2, (int) vlanConfigurationCacheImpl.assignInternalVlan(mock(Node.class), NETWORK_ID + "1"));
        }

    @Test
    public void testReclaimInternalVlan(){
        setNodeConfiguration();

        assertEquals(1, (int) vlanConfigurationCacheImpl.reclaimInternalVlan(any(Node.class), NETWORK_ID));
        assertEquals(2, (int) vlanConfigurationCacheImpl.reclaimInternalVlan(any(Node.class), NETWORK_ID + "1"));
    }
}
