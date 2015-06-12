/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbNodeRemoveCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    Map<ConnectionInfo,OvsdbConnectionInstance> clients
        = new ConcurrentHashMap<ConnectionInfo,OvsdbConnectionInstance>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);

    private DataBroker db;
    private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers =
            new ConcurrentHashMap<ConnectionInfo,InstanceIdentifier<Node>>();

    public OvsdbConnectionManager(DataBroker db,TransactionInvoker txInvoker) {
        this.db = db;
        this.txInvoker = txInvoker;
    }

    @Override
    public void connected(final OvsdbClient externalClient) {
        OvsdbConnectionInstance client = connectedButCallBacksNotRegistered(externalClient);
        client.registerCallbacks();
    }

    public OvsdbConnectionInstance connectedButCallBacksNotRegistered(final OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        ConnectionInfo key = SouthboundMapper.createConnectionInfo(externalClient);
        OvsdbConnectionInstance client = new OvsdbConnectionInstance(key,externalClient,txInvoker,
                getInstanceIdentifier(key));
        putConnectionInstance(key, client);
        client.createTransactInvokers();
        return client;
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnect from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        ConnectionInfo key = SouthboundMapper.createConnectionInfo(client);
        txInvoker.invoke(new OvsdbNodeRemoveCommand(getConnectionInstance(key),null,null));
        clients.remove(key);
        LOG.trace("OvsdbConnectionManager: disconnected exit");
    }

    public OvsdbClient connect(InstanceIdentifier<Node> iid, OvsdbNodeAugmentation ovsdbNode)
            throws UnknownHostException {
        // TODO handle case where we already have a connection
        // TODO use transaction chains to handle ordering issues between
        // disconnected
        // and connected when writing to the operational store
        InetAddress ip = SouthboundMapper.createInetAddress(ovsdbNode.getConnectionInfo().getRemoteIp());
        OvsdbClient client = null;
        try {
            client = OvsdbConnectionService.getService().connect(ip,
                    ovsdbNode.getConnectionInfo().getRemotePort().getValue().intValue());
            putInstanceIdentifier(ovsdbNode.getConnectionInfo(), iid.firstIdentifierOf(Node.class));
            // For connections from the controller to the ovs instance, the
            // library doesn't call this method for us
            connectedButCallBacksNotRegistered(client);
        } catch (Exception e) {
            LOG.error("Failed to connect", e);
        }
        return client;
    }

    public void disconnect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        OvsdbClient client = getConnectionInstance(ovsdbNode.getConnectionInfo());
        if (client != null) {
            client.disconnect();
        }
    }

    public void init(ConnectionInfo key) {
        OvsdbConnectionInstance client = getConnectionInstance(key);
        if (client != null) {
            /*
             *  Note: registerCallbacks() is idemPotent... so if you call it repeatedly all is safe,
             *  it only registersCallbacks on the *first* call.
             */
            client.registerCallbacks();
        }
    }

    @Override
    public void close() throws Exception {
        for (OvsdbClient client: clients.values()) {
            client.disconnect();
        }
    }

    private void putConnectionInstance(ConnectionInfo key,OvsdbConnectionInstance instance) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
    }

    private void putInstanceIdentifier(ConnectionInfo key,InstanceIdentifier<Node> iid) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.put(connectionInfo, iid);
    }

    public OvsdbConnectionInstance getConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
    }

    public InstanceIdentifier<Node> getInstanceIdentifier(ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        InstanceIdentifier<Node> iid = instanceIdentifiers.get(connectionInfo);
        return iid;
    }

    public OvsdbConnectionInstance getConnectionInstance(OvsdbBridgeAttributes mn) {
        Optional<OvsdbNodeAugmentation> optional = SouthboundUtil.getManagingNode(db, mn);
        if (optional.isPresent()) {
            return getConnectionInstance(optional.get().getConnectionInfo());
        } else {
            return null;
        }
    }

    public OvsdbConnectionInstance getConnectionInstance(Node node) {
        Preconditions.checkNotNull(node);
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        OvsdbBridgeAugmentation ovsdbManagedNode = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNode != null) {
            return getConnectionInstance(ovsdbNode.getConnectionInfo());
        } else if (ovsdbManagedNode != null) {
            return getConnectionInstance(ovsdbManagedNode);
        } else {
            LOG.warn("This is not a node that gives any hint how to find its OVSDB Manager: {}",node);
            return null;
        }
    }

    public OvsdbConnectionInstance getConnectionInstance(InstanceIdentifier<Node> nodePath) {
        try {
            ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
            CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture = transaction.read(
                    LogicalDatastoreType.OPERATIONAL, nodePath);
            transaction.close();
            Optional<Node> optional = nodeFuture.get();
            if (optional != null && optional.isPresent() && optional.get() instanceof Node) {
                return this.getConnectionInstance(optional.get());
            } else {
                LOG.warn("Found non-topological node {} on path {}",optional);
                return null;
            }
        } catch (Exception e) {
            LOG.warn("Failed to get Ovsdb Node {}",nodePath, e);
            return null;
        }
    }

    public OvsdbClient getClient(ConnectionInfo connectionInfo) {
        return getConnectionInstance(connectionInfo);
    }

    public OvsdbClient getClient(OvsdbBridgeAttributes mn) {
        return getConnectionInstance(mn);
    }

    public OvsdbClient getClient(Node node) {
        return getConnectionInstance(node);
    }
}
