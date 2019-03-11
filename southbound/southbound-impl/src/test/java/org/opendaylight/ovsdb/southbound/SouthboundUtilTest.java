/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.InetAddress;
import java.net.NetworkInterface;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SouthboundUtil.class, NetworkInterface.class})
public class SouthboundUtilTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SouthboundUtil.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetManagingNode() throws Exception {
        OvsdbBridgeAttributes mn = mock(OvsdbBridgeAttributes.class);
        DataBroker db = mock(DataBroker.class);
        OvsdbNodeRef ref = mock(OvsdbNodeRef.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(db.newReadOnlyTransaction()).thenReturn(transaction);
        when(mn.getManagedBy()).thenReturn(ref);
        when(ref.getValue()).thenAnswer(
                (Answer<InstanceIdentifier<Node>>) invocation -> (InstanceIdentifier<Node>) mock(
                        InstanceIdentifier.class));
        CheckedFuture<Optional<Node>, ReadFailedException> nf = mock(CheckedFuture.class);
        when(transaction.read(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class))).thenReturn(nf);
        doNothing().when(transaction).close();

        //node, ovsdbNode not null
        Node node = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        when(nf.get()).thenReturn(Optional.of(node));
        when(node.augmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNode);
        assertEquals("Failed to return correct Optional object", Optional.of(ovsdbNode),
                SouthboundUtil.getManagingNode(db, mn));

        //node not null, ovsdbNode null
        when(nf.get()).thenReturn(Optional.absent());
        assertEquals("Failed to return correct Optional object", Optional.absent(),
                SouthboundUtil.getManagingNode(db, mn));

        //optional null
        when(nf.get()).thenReturn(null);
        assertEquals("Failed to return correct Optional object", Optional.absent(),
                SouthboundUtil.getManagingNode(db, mn));

        //ref null
        when(mn.getManagedBy()).thenReturn(null);
        assertEquals("Failed to return correct Optional object", Optional.absent(),
                SouthboundUtil.getManagingNode(db, mn));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadNode() throws Exception {
        Optional<DataObject> node = Optional.of(mock(DataObject.class));
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        InstanceIdentifier<DataObject> connectionIid = mock(InstanceIdentifier.class);
        CheckedFuture<Optional<DataObject>, ReadFailedException> value = mock(CheckedFuture.class);
        when(transaction.read(LogicalDatastoreType.OPERATIONAL, connectionIid)).thenReturn(value);
        when(value.checkedGet()).thenReturn(node);
        assertEquals("Incorrect Optional object received", node, SouthboundUtil.readNode(transaction, connectionIid));
    }

    @Test
    public void testGetLocalControllerHostIpAddress() throws Exception {

        //NetworkInterface.getNetworkInterfaces() returns null case
        PowerMockito.mockStatic(NetworkInterface.class);
        when(NetworkInterface.getNetworkInterfaces()).thenReturn(null);
        assertNull(SouthboundUtil.getLocalControllerHostIpAddress());

        InetAddress inetAddr = mock(InetAddress.class);
        when(inetAddr.isLoopbackAddress()).thenReturn(false);
        when(inetAddr.isSiteLocalAddress()).thenReturn(true);
        when(inetAddr.getHostAddress()).thenReturn("HostAddress");

        NetworkInterface iface = PowerMockito.mock(NetworkInterface.class);
        when(iface.getInetAddresses()).thenReturn(Iterators.asEnumeration(
            Iterators.singletonIterator(inetAddr)));

        when(NetworkInterface.getNetworkInterfaces()).thenReturn(Iterators.asEnumeration(
            Iterators.singletonIterator(iface)));

        assertEquals("HostAddress", SouthboundUtil.getLocalControllerHostIpAddress());
    }

    @Test
    public void testGetControllerTarget() throws Exception {
        Node ovsdbNode = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.augmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class, Mockito.RETURNS_DEEP_STUBS);
        when(ovsdbNodeAugmentation.getConnectionInfo()).thenReturn(connectionInfo);

        //ipAddr not null case
        IpAddress ipAddr = new IpAddress(new Ipv4Address("0.0.0.0"));
        when(connectionInfo.getLocalIp()).thenReturn(ipAddr);
        String testTarget = SouthboundConstants.OPENFLOW_CONNECTION_PROTOCOL + ":"
                + "0.0.0.0" + ":" + SouthboundConstants.DEFAULT_OPENFLOW_PORT;
        assertEquals("Incorrect controller IP", testTarget, SouthboundUtil.getControllerTarget(ovsdbNode));
        verify(ovsdbNode).augmentation(OvsdbNodeAugmentation.class);
        verify(ovsdbNodeAugmentation).getConnectionInfo();

        //ipAddr null case
        when(connectionInfo.getLocalIp()).thenReturn(null);

        //suppress call to getLocalControllerHostIpAddress()
        PowerMockito.doReturn("127.0.0.1").when(SouthboundUtil.class, "getLocalControllerHostIpAddress");
        testTarget = SouthboundConstants.OPENFLOW_CONNECTION_PROTOCOL + ":"
                + "127.0.0.1" + ":" + SouthboundConstants.DEFAULT_OPENFLOW_PORT;
        assertEquals("Incorrect Local controller host IP", testTarget, SouthboundUtil.getControllerTarget(ovsdbNode));
    }
}
