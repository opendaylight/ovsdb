/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.mockito.Mockito.mock;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Unit tests for the data-tree change listener.
 */
public class OvsdbDataTreeChangeListenerTest extends AbstractDataBrokerTest {

    private final OvsdbConnection ovsdbConnection = mock(OvsdbConnection.class);
    private DataBroker dataBroker;
    private OvsdbDataTreeChangeListener listener;

    @Before
    public void setupListener() {
        dataBroker = getDataBroker();
        EntityOwnershipService entityOwnershipService = mock(EntityOwnershipService.class);
        InstanceIdentifierCodec instanceIdentifierCodec = mock(InstanceIdentifierCodec.class);
        listener = new OvsdbDataTreeChangeListener(dataBroker,
                new OvsdbConnectionManager(dataBroker, new TransactionInvokerImpl(dataBroker), entityOwnershipService,
                        ovsdbConnection, instanceIdentifierCodec), instanceIdentifierCodec);
    }

    @Test
    public void testConnect() throws UnknownHostException, InterruptedException, ExecutionException {
        // Given ...

        // When we request a connection ...
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
        int port = 6640;
        IpAddress ipAddress = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber portNumber = new PortNumber(port);

        final ConnectionInfo connectionInfo = new ConnectionInfoBuilder()
                .setRemoteIp(ipAddress)
                .setRemotePort(portNumber)
                .build();
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, SouthboundUtils.createNode(connectionInfo),
                WriteTransaction.CREATE_MISSING_PARENTS);
        transaction.submit().get();

        // Then the listener tries to open a connection
        Mockito.verify(ovsdbConnection).connect(inetAddress, port);
    }
}
