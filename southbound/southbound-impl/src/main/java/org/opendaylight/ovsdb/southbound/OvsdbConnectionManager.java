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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    Map<OvsdbClientKey,OvsdbConnectionInstance> clients = new ConcurrentHashMap<OvsdbClientKey,OvsdbConnectionInstance>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);

    DataBroker db;

    public OvsdbConnectionManager(DataBroker db) {
        this.db = db;
    }

    @Override
    public void connected(OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        OvsdbClientKey key = new OvsdbClientKey(externalClient);
        OvsdbConnectionInstance client = new OvsdbConnectionInstance(key,externalClient);
        clients.put(key, client);
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, key.toInstanceIndentifier(),
                SouthboundMapper.createNode(client));
        // TODO - Check the future and retry if needed
        transaction.submit();
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnect from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        OvsdbClientKey key = new OvsdbClientKey(client);
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, key.toInstanceIndentifier());
        // TODO - Check the future and retry if needed
        transaction.submit();
        clients.remove(key);
    }

    public OvsdbClient connect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        // TODO handle case where we already have a connection
        // TODO use transaction chains to handle ordering issues between disconnected and connected when writing to the operational store
        InetAddress ip = SouthboundMapper.createInetAddress(ovsdbNode.getIp());
        OvsdbClient client = OvsdbConnectionService.getService().connect(ip, ovsdbNode.getPort().getValue().intValue());
        OvsdbClientKey key = new OvsdbClientKey(client);
        OvsdbConnectionInstance instance = new OvsdbConnectionInstance(key,client);
        clients.put(key, instance);
        connected(client); // For connections from the controller to the ovs instance, the library doesn't call this method for us
        return client;
    }

    public void disconnect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        OvsdbClientKey key = new OvsdbClientKey(ovsdbNode.getIp(), ovsdbNode.getPort());
        OvsdbClient client = clients.get(key);
        if (client != null) {
            client.disconnect();
            disconnected(client);
        }
    }

    @Override
    public void close() throws Exception {
        for(OvsdbClient client: clients.values()) {
            client.disconnect();
        }
    }


}
