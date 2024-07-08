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
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.operations.DefaultOperations;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * Unit tests for the data-tree change listener.
 */
public class OvsdbDataTreeChangeListenerTest extends AbstractConcurrentDataBrokerTest {

    private final OvsdbConnection ovsdbConnection = mock(OvsdbConnection.class);
    private DataBroker dataBroker;
    private OvsdbDataTreeChangeListener listener;

    public OvsdbDataTreeChangeListenerTest() {
        super(true);
    }

    @Before
    public void setupListener() {
        dataBroker = getDataBroker();
        EntityOwnershipService entityOwnershipService = mock(EntityOwnershipService.class);
        InstanceIdentifierCodec instanceIdentifierCodec = mock(InstanceIdentifierCodec.class);
        listener = new OvsdbDataTreeChangeListener(dataBroker,
                new OvsdbConnectionManager(dataBroker, new DefaultOperations(), new TransactionInvokerImpl(dataBroker),
                    entityOwnershipService, ovsdbConnection, instanceIdentifierCodec, List.of(), List.of()),
                instanceIdentifierCodec);
    }

    @Test
    public void testConnect() throws UnknownHostException, InterruptedException, ExecutionException {
        // Given ...

        // When we request a connection ...
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
        int port = 6640;
        IpAddress ipAddress = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber portNumber = new PortNumber(Uint16.valueOf(port));

        final ConnectionInfo connectionInfo = new ConnectionInfoBuilder()
                .setRemoteIp(ipAddress)
                .setRemotePort(portNumber)
                .build();
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        WriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, iid,
            SouthboundUtils.createNode(connectionInfo));
        transaction.commit().get();

        // Then the listener tries to open a connection
        Mockito.verify(ovsdbConnection, Mockito.timeout(5000)).connect(inetAddress, port);
    }
}
