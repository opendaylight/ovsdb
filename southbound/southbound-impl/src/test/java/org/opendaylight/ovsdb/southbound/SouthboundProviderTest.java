package org.opendaylight.ovsdb.southbound;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

@PrepareForTest({SouthboundProvider.class, InstanceIdentifier.class, LogicalDatastoreType.class})
@RunWith(PowerMockRunner.class)
public class SouthboundProviderTest {
    @Mock private DataBroker db;
    @Mock private OvsdbConnectionManager cm;
    @Mock private TransactionInvoker txInvoker;
    @Mock private OvsdbDataChangeListener ovsdbDataChangeListener;
    @Mock private SouthboundProvider southboundProvider;


    @Before
    public void setUp() throws Exception {
        southboundProvider = PowerMockito.mock(SouthboundProvider.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(SouthboundProvider.class, "cm").set(southboundProvider, cm);
        MemberModifier.field(SouthboundProvider.class, "ovsdbDataChangeListener").set(southboundProvider, ovsdbDataChangeListener);
        MemberModifier.field(SouthboundProvider.class, "db").set(southboundProvider, db);
    }

    @Test
    public void testOnSessionInitiated() throws Exception {
        ProviderContext session = mock(ProviderContext.class);
        when(session.getSALService(DataBroker.class)).thenReturn(db);
        TransactionInvokerImpl transactionInvokerImpl = mock(TransactionInvokerImpl.class);
        PowerMockito.whenNew(TransactionInvokerImpl.class).withArguments(any(DataBroker.class)).thenReturn(transactionInvokerImpl);
        PowerMockito.whenNew(OvsdbConnectionManager.class).withArguments(any(DataBroker.class), any(TransactionInvoker.class)).thenReturn(cm);
        PowerMockito.whenNew(OvsdbDataChangeListener.class).withArguments(any(DataBroker.class), any(OvsdbConnectionManager.class)).thenReturn(ovsdbDataChangeListener);

        //suppress calls to initializeOvsdbTopology()
        MemberModifier.suppress(MemberMatcher.method(SouthboundProvider.class, "initializeOvsdbTopology", LogicalDatastoreType.class));

        OvsdbConnection ovsdbConnection = mock(OvsdbConnectionService.class);
        PowerMockito.whenNew(OvsdbConnectionService.class).withNoArguments().thenReturn((OvsdbConnectionService) ovsdbConnection);
        doNothing().when(ovsdbConnection).registerConnectionListener(cm);
        when(ovsdbConnection.startOvsdbManager(any(Integer.class))).thenReturn(true);
        southboundProvider.onSessionInitiated(session);

        verify(ovsdbConnection).registerConnectionListener(any(OvsdbConnectionManager.class));
        verify(ovsdbConnection).startOvsdbManager(any(Integer.class));
        PowerMockito.verifyPrivate(southboundProvider, times(2)).invoke("initializeOvsdbTopology", any(LogicalDatastoreType.class));
    }

    @Test
    public void testClose() throws Exception {
        doNothing().when(cm).close();
        doNothing().when(ovsdbDataChangeListener).close();
        southboundProvider.close();
        verify(cm).close();
        verify(ovsdbDataChangeListener).close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeTopology() throws Exception {
        InstanceIdentifier<NetworkTopology> path = mock(InstanceIdentifier.class);
        PowerMockito.mockStatic(InstanceIdentifier.class);
        when(InstanceIdentifier.create(NetworkTopology.class)).thenReturn(path);

        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = mock(CheckedFuture.class);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(topology);

        Optional<NetworkTopology> optNetTopo = mock(Optional.class);
        when(topology.get()).thenReturn(optNetTopo);
        when(optNetTopo.isPresent()).thenReturn(false);
        NetworkTopologyBuilder ntb = mock(NetworkTopologyBuilder.class);
        PowerMockito.whenNew(NetworkTopologyBuilder.class).withNoArguments().thenReturn(ntb);
        NetworkTopology networkTopology = mock(NetworkTopology.class);
        when(ntb.build()).thenReturn(networkTopology);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(NetworkTopology.class));

        LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        Whitebox.invokeMethod(southboundProvider, "initializeTopology", transaction, type);
        verify(ntb).build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeOvsdbTopology() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(db.newReadWriteTransaction()).thenReturn(transaction);

        //suppress calls to initializeTopology()
        MemberModifier.suppress(MemberMatcher.method(SouthboundProvider.class, "initializeTopology", ReadWriteTransaction.class, LogicalDatastoreType.class));

        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(ovsdbTp);

        //true case
        Optional<Topology> optTopo = mock(Optional.class);
        when(ovsdbTp.get()).thenReturn(optTopo);
        when(optTopo.isPresent()).thenReturn(false);
        TopologyBuilder tpb = mock(TopologyBuilder.class);
        PowerMockito.whenNew(TopologyBuilder.class).withNoArguments().thenReturn(tpb);
        when(tpb.setTopologyId(any(TopologyId.class))).thenReturn(tpb);
        Topology data = mock(Topology.class);
        when(tpb.build()).thenReturn(data);
        doNothing().when(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Topology.class));
        when(transaction.submit()).thenReturn(mock(CheckedFuture.class));

        LogicalDatastoreType type = PowerMockito.mock(LogicalDatastoreType.class);
        Whitebox.invokeMethod(southboundProvider, "initializeOvsdbTopology", type);
        PowerMockito.verifyPrivate(southboundProvider).invoke("initializeTopology", any(ReadWriteTransaction.class), any(LogicalDatastoreType.class));
        verify(tpb).setTopologyId(any(TopologyId.class));
        verify(tpb).build();

        //false case
        when(optTopo.isPresent()).thenReturn(false);
        when(transaction.cancel()).thenReturn(true);
        Whitebox.invokeMethod(southboundProvider, "initializeOvsdbTopology", type);
        PowerMockito.verifyPrivate(southboundProvider, times(2)).invoke("initializeTopology", any(ReadWriteTransaction.class), any(LogicalDatastoreType.class));
    }
}