/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SouthboundUtil.class, NetworkInterface.class})
public class SouthboundUtilTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SouthboundUtil.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testSetInstanceIdentifierCodec() throws Exception {
        InstanceIdentifierCodec iidc = mock(InstanceIdentifierCodec.class);
        SouthboundUtil.setInstanceIdentifierCodec(iidc);
        assertEquals("InstanceIdentifierCodec object not correctly set", iidc,
                SouthboundUtil.getInstanceIdentifierCodec());
    }

    @Test
    public void testSerializeInstanceIdentifier() throws Exception {
        InstanceIdentifier<?> iid = mock(InstanceIdentifier.class);
        InstanceIdentifierCodec iidc = (InstanceIdentifierCodec) setField("instanceIdentifierCodec",
                mock(InstanceIdentifierCodec.class));
        when(iidc.serialize(iid)).thenReturn("serializeInstanceIdentifier");
        assertEquals("Incorrect String returned", "serializeInstanceIdentifier",
                SouthboundUtil.serializeInstanceIdentifier(iid));
        verify(iidc).serialize(any(InstanceIdentifier.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testDeserializeInstanceIdentifier() throws Exception {
        InstanceIdentifier result = mock(InstanceIdentifier.class);
        InstanceIdentifierCodec iidc = (InstanceIdentifierCodec) setField("instanceIdentifierCodec",
                mock(InstanceIdentifierCodec.class));
        when(iidc.bindingDeserializer(anyString())).thenReturn(result);
        assertEquals(result, SouthboundUtil.deserializeInstanceIdentifier("iidString"));
        verify(iidc).bindingDeserializer(anyString());
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
        when(ref.getValue()).thenAnswer(new Answer<InstanceIdentifier<Node>>() {
            public InstanceIdentifier<Node> answer(InvocationOnMock invocation) throws Exception {
                return (InstanceIdentifier<Node>) mock(InstanceIdentifier.class);
            }
        });
        CheckedFuture<Optional<Node>, ReadFailedException> nf = mock(CheckedFuture.class);
        when(transaction.read(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class))).thenReturn(nf);
        doNothing().when(transaction).close();

        //node, ovsdbNode not null
        Node node = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        when(nf.get()).thenReturn(Optional.of(node));
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNode);
        assertEquals("Failed to return correct Optional object", Optional.of(ovsdbNode),
                SouthboundUtil.getManagingNode(db, mn));

        //node not null, ovsdbNode null
        when(nf.get()).thenReturn(Optional.<Node>absent());
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

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLocalControllerHostIpAddress() throws Exception {

        //NetworkInterface.getNetworkInterfaces() returns null case
        PowerMockito.mockStatic(NetworkInterface.class);
        when(NetworkInterface.getNetworkInterfaces()).thenReturn(null);
        assertEquals(null, Whitebox.invokeMethod(SouthboundUtil.class, "getLocalControllerHostIpAddress"));

        Enumeration<NetworkInterface> ifaces = mock(Enumeration.class);
        when(NetworkInterface.getNetworkInterfaces()).thenReturn(ifaces);
        when(ifaces.hasMoreElements()).thenReturn(true).thenReturn(false);
        NetworkInterface iface = PowerMockito.mock(NetworkInterface.class);
        when(ifaces.nextElement()).thenReturn(iface);

        Enumeration<InetAddress> inetAddrs = mock(Enumeration.class);
        when(iface.getInetAddresses()).thenReturn(inetAddrs);
        when(inetAddrs.hasMoreElements()).thenReturn(true).thenReturn(false);
        InetAddress inetAddr = mock(InetAddress.class);
        when(inetAddrs.nextElement()).thenReturn(inetAddr);
        when(inetAddr.isLoopbackAddress()).thenReturn(false);
        when(inetAddr.isSiteLocalAddress()).thenReturn(true);
        when(inetAddr.getHostAddress()).thenReturn("HostAddress");
        assertEquals("HostAddress", Whitebox.invokeMethod(SouthboundUtil.class, "getLocalControllerHostIpAddress"));
    }

    @Test
    public void testGetControllerTarget() throws Exception {
        Node ovsdbNode = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class, Mockito.RETURNS_DEEP_STUBS);
        when(ovsdbNodeAugmentation.getConnectionInfo()).thenReturn(connectionInfo);

        //ipAddr not null case
        IpAddress ipAddr = mock(IpAddress.class);
        when(connectionInfo.getLocalIp()).thenReturn(ipAddr);
        char[] ipAddress = {'0', '.', '0', '.', '0', '.', '0'};
        when(connectionInfo.getLocalIp().getValue()).thenReturn(ipAddress);
        String testTarget = SouthboundConstants.OPENFLOW_CONNECTION_PROTOCOL + ":"
                + String.valueOf(ipAddress) + ":" + SouthboundConstants.DEFAULT_OPENFLOW_PORT;
        assertEquals("Incorrect controller IP", testTarget, SouthboundUtil.getControllerTarget(ovsdbNode));
        verify(ovsdbNode).getAugmentation(OvsdbNodeAugmentation.class);
        verify(ovsdbNodeAugmentation).getConnectionInfo();

        //ipAddr null case
        when(connectionInfo.getLocalIp()).thenReturn(null);

        //suppress call to getLocalControllerHostIpAddress()
        MemberModifier.suppress(MemberMatcher.method(SouthboundUtil.class, "getLocalControllerHostIpAddress"));
        PowerMockito.when(SouthboundUtil.class, "getLocalControllerHostIpAddress").thenReturn("127.0.0.1");
        testTarget = SouthboundConstants.OPENFLOW_CONNECTION_PROTOCOL + ":"
                + "127.0.0.1" + ":" + SouthboundConstants.DEFAULT_OPENFLOW_PORT;
        assertEquals("Incorrect Local controller host IP", testTarget, SouthboundUtil.getControllerTarget(ovsdbNode));
    }

    private Object getField(String fieldName) throws Exception {
        Field field = SouthboundUtil.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(SouthboundUtil.class);
    }

    private Object setField(String fieldName, InstanceIdentifierCodec fieldValue) throws Exception {
        Field field = SouthboundUtil.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(SouthboundUtil.class), fieldValue);
        return field.get(SouthboundUtil.class);
    }
}
