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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    Map<OvsdbClientKey,OvsdbConnectionInstance> clients = new ConcurrentHashMap<OvsdbClientKey,OvsdbConnectionInstance>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);

    private DataBroker db;
    private TransactionInvoker txInvoker;

    public OvsdbConnectionManager(DataBroker db,TransactionInvoker txInvoker) {
        this.db = db;
        this.txInvoker = txInvoker;
    }

    @Override
    public void connected(final OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        OvsdbClientKey key = new OvsdbClientKey(externalClient);
        OvsdbConnectionInstance client = new OvsdbConnectionInstance(key,externalClient,txInvoker);
        clients.put(key, client);
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnect from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        OvsdbClientKey key = new OvsdbClientKey(client);
        txInvoker.invoke(new OvsdbNodeRemoveCommand(key,null,null));
        clients.remove(key);
    }

    public OvsdbClient connect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        // TODO handle case where we already have a connection
        // TODO use transaction chains to handle ordering issues between disconnected and connected when writing to the operational store
        InetAddress ip = SouthboundMapper.createInetAddress(ovsdbNode.getIp());
        OvsdbClient client = OvsdbConnectionService.getService().connect(ip, ovsdbNode.getPort().getValue().intValue());
        connected(client); // For connections from the controller to the ovs instance, the library doesn't call this method for us
        return client;
    }

    public void disconnect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        OvsdbClientKey key = new OvsdbClientKey(ovsdbNode.getIp(), ovsdbNode.getPort());
        OvsdbClient client = clients.get(key);
        if (client != null) {
            client.disconnect();
        }
    }

    @Override
    public void close() throws Exception {
        for(OvsdbClient client: clients.values()) {
            client.disconnect();
        }
    }

    public OvsdbConnectionInstance getConnectionInstance(OvsdbClientKey key) {
        return clients.get(key);
    }

    public OvsdbConnectionInstance getConnectionInstance(IpPortLocator loc) {
        Preconditions.checkNotNull(loc);
        return getConnectionInstance(new OvsdbClientKey(loc));
    }

    public OvsdbConnectionInstance getConnectionInstance(OvsdbBridgeAttributes mn) {
        Preconditions.checkNotNull(mn);
        try {
            OvsdbNodeRef ref = mn.getManagedBy();
            if(ref != null) {
                ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
                CheckedFuture<?, ReadFailedException> nf = transaction.read(LogicalDatastoreType.OPERATIONAL, ref.getValue());
                transaction.close();
                Object obj = nf.get();
                if(obj instanceof Node) {
                    OvsdbNodeAugmentation ovsdbNode = ((Node)obj).getAugmentation(OvsdbNodeAugmentation.class);
                    if(ovsdbNode !=null) {
                        return getConnectionInstance(ovsdbNode);
                    } else {
                        LOG.warn("OvsdbManagedNode {} claims to be managed by {} but that OvsdbNode does not exist",mn,ref.getValue());
                        return null;
                    }
                } else {
                    LOG.warn("Mysteriously got back a thing which is *not* a topology Node: {}",obj);
                    return null;
                }
            } else {
                LOG.warn("Cannot find client for OvsdbManagedNode without a specified ManagedBy {}",mn);
                return null;
            }
         } catch (Exception e) {
             LOG.warn("Failed to get OvsdbNode that manages OvsdbManagedNode {}",mn, e);
             return null;
         }
    }

    public OvsdbConnectionInstance getConnectionInstance(Node node) {
        Preconditions.checkNotNull(node);
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        OvsdbManagedNodeAugmentation ovsdbManagedNode = node.getAugmentation(OvsdbManagedNodeAugmentation.class);
        if(ovsdbNode != null) {
            return getConnectionInstance(ovsdbNode);
        } else if (ovsdbManagedNode != null) {
            return getConnectionInstance(ovsdbManagedNode);
        } else {
            LOG.warn("This is not a node that gives any hint how to find its OVSDB Manager: {}",node);
            return null;
        }
    }

    public OvsdbClient getClient(OvsdbClientKey key) {
        return getConnectionInstance(key);
    }

    public OvsdbClient getClient(IpPortLocator loc) {
        return getConnectionInstance(loc);
    }

    public OvsdbClient getClient(OvsdbBridgeAttributes mn) {
        return getConnectionInstance(mn);
    }

    public OvsdbClient getClient(Node node) {
        return getConnectionInstance(node);
    }
}
