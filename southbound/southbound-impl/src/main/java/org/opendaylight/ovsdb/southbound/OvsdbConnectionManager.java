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
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

import javax.annotation.Nonnull;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    private Map<ConnectionInfo, OvsdbConnectionInstance> clients =
            new ConcurrentHashMap<ConnectionInfo,OvsdbConnectionInstance>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);
    private static final String ENTITY_TYPE = "ovsdb";

    private DataBroker db;
    private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers =
            new ConcurrentHashMap<ConnectionInfo,InstanceIdentifier<Node>>();
    private Map<InstanceIdentifier<Node>, OvsdbConnectionInstance> candidateConnectionIidMap =
            new ConcurrentHashMap<>();
    private EntityOwnershipService entityOwnershipService;
    private OvsdbDeviceEntityOwnershipListener ovsdbDeviceEntityOwnershipListener;

    public OvsdbConnectionManager(DataBroker db,TransactionInvoker txInvoker,
                                  EntityOwnershipService entityOwnershipService) {
        this.db = db;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        this.ovsdbDeviceEntityOwnershipListener = new OvsdbDeviceEntityOwnershipListener(this, entityOwnershipService);
    }

    @Override
    public void connected(@Nonnull final OvsdbClient externalClient) {

        final ConnectionInfo key = SouthboundMapper.createConnectionInfo(externalClient);

        // FIXME (FF): need to add instance identifier to instanceIdentifiers map before calling
        // FIXME (FF): connectedButCallBacksNotRegistered! Failure to do that would end up with
        // FIXME (FF): an OvsdbConnectionInstance that has instanceIdentifier field st to null.
        // putInstanceIdentifier(key, ??? iid.firstIdentifierOf(Node.class));

        OvsdbConnectionInstance client = connectedButCallBacksNotRegistered(externalClient);

        candidateConnectionIidMap.put(client.getInstanceIdentifier(), client);

        // Register Cluster Onwership for ConnectionInfo
        try {
            entityOwnershipService.registerCandidate(getEntityFromConnectionInstance(client));
            LOG.info("OVSDB entity registration done for {}", key);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB entity registration for {} already exists", key, e);
        }

        // client.registerCallbacks();  // now done upon onwership callback!
    }

    public OvsdbConnectionInstance connectedButCallBacksNotRegistered(final OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        ConnectionInfo key = SouthboundMapper.createConnectionInfo(externalClient);
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(key);

        // Sanity: the block below handles unexpected cases when there is an existing ovsdbConnectionInstance
        //         for the OvsdbClient provided. In such cases, we will see if the ovsdbConnectionInstance
        //         has same externalClient.
        if (ovsdbConnectionInstance != null) {
            if (ovsdbConnectionInstance.hasOvsdbClient(externalClient)) {
                LOG.warn("OVSDB Connection Instance {} already exists for client {}", key, externalClient);
                return ovsdbConnectionInstance;
            }
            LOG.warn("OVSDB Connection Instance {} being replaced with client {}", key, externalClient);
            ovsdbConnectionInstance.disconnect();

            // Unregister Cluster Onwership for ConnectionInfo
            // Because the ovsdbConnectionInstance is about to be completely replaced!
            ovsdbConnectionInstance.closeDeviceOwnershipCandidateRegistration();
            candidateConnectionIidMap.remove(ovsdbConnectionInstance.getInstanceIdentifier());

            removeConnectionInstance(key);
        }

        OvsdbConnectionInstance ovsdbConnectionInstance2 = new OvsdbConnectionInstance(key, externalClient, txInvoker,
                getInstanceIdentifier(key));

        // putConnectionInstance(key, ovsdbConnectionInstance2);  // now done upon onwership callback!
        // ovsdbConnectionInstance2.createTransactInvokers();     // now done upon onwership callback!

        return ovsdbConnectionInstance2;
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnected from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        ConnectionInfo key = SouthboundMapper.createConnectionInfo(client);
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(key);
        if (ovsdbConnectionInstance != null) {
            txInvoker.invoke(new OvsdbNodeRemoveCommand(ovsdbConnectionInstance, null, null));
            removeConnectionInstance(key);
            // Unregister Cluster Onwership for ConnectionInfo
            ovsdbConnectionInstance.closeDeviceOwnershipCandidateRegistration();
            candidateConnectionIidMap.remove(ovsdbConnectionInstance.getInstanceIdentifier());
        } else {
            LOG.warn("OVSDB disconnected event did not find connection instance for {}", key);
        }
        LOG.trace("OvsdbConnectionManager: disconnected exit");
    }

    public OvsdbClient connect(InstanceIdentifier<Node> iid,
            OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        // TODO handle case where we already have a connection
        // TODO use transaction chains to handle ordering issues between disconnected
        // TODO and connected when writing to the operational store
        InetAddress ip = SouthboundMapper.createInetAddress(ovsdbNode.getConnectionInfo().getRemoteIp());
        OvsdbClient client = OvsdbConnectionService.getService().connect(ip,
                ovsdbNode.getConnectionInfo().getRemotePort().getValue());
        // For connections from the controller to the ovs instance, the library doesn't call
        // this method for us
        if (client != null) {
            putInstanceIdentifier(ovsdbNode.getConnectionInfo(), iid.firstIdentifierOf(Node.class));
            OvsdbConnectionInstance ovsdbConnectionInstance = connectedButCallBacksNotRegistered(client);

            // FIXME (FF): the 2 lines below were moved out of connectedButCallBacksNotRegistered in order
            // FIXME (FF): to support EntityOwnershipService. The same logic may need to be applied here, but
            // FIXME (FF): that is to be visited in the very near future.
            putConnectionInstance(ovsdbNode.getConnectionInfo(),
                    ovsdbConnectionInstance); // from connectedButCallBacksNotRegistered
            ovsdbConnectionInstance.createTransactInvokers(); // from connectedButCallBacksNotRegistered

        } else {
            LOG.warn("Failed to connect to Ovsdb Node {}", ovsdbNode.getConnectionInfo());
        }
        return client;
    }

    public void disconnect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        OvsdbConnectionInstance client = getConnectionInstance(ovsdbNode.getConnectionInfo());
        if (client != null) {
            client.disconnect();

            // Unregister Cluster Onwership for ConnectionInfo
            client.closeDeviceOwnershipCandidateRegistration();
            candidateConnectionIidMap.remove(client.getInstanceIdentifier());

            removeInstanceIdentifier(ovsdbNode.getConnectionInfo());
        }
    }

    public void init(ConnectionInfo key) {
        OvsdbConnectionInstance client = getConnectionInstance(key);

        // TODO (FF): make sure that this cluster instance is the 'entity owner' fo the given OvsdbConnectionInstance ?

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
        if (ovsdbDeviceEntityOwnershipListener != null) {
            // TODO: ovsdbDeviceEntityOwnershipListener.close();
        }
    }

    private void putConnectionInstance(ConnectionInfo key,OvsdbConnectionInstance instance) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
    }

    private void removeConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        clients.remove(connectionInfo);
    }

    private void putInstanceIdentifier(ConnectionInfo key,InstanceIdentifier<Node> iid) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.put(connectionInfo, iid);
    }

    private void removeInstanceIdentifier(ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.remove(connectionInfo);
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

    public Boolean getHasDeviceOwnership(ConnectionInfo connectionInfo) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(connectionInfo);
        if (ovsdbConnectionInstance == null) {
            return Boolean.FALSE;
        }
        return ovsdbConnectionInstance.getHasDeviceOwnership();
    }

    public void setHasDeviceOwnership(ConnectionInfo connectionInfo, Boolean hasDeviceOwnership) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(connectionInfo);
        if (ovsdbConnectionInstance != null) {
            ovsdbConnectionInstance.setHasDeviceOwnership(hasDeviceOwnership);
        }
    }

    private void handleOwnershipChanged(EntityOwnershipChange ownershipChange) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstanceFromEntity(ownershipChange.getEntity());
        LOG.info("handleOwnershipChanged: {} instance: {}", ownershipChange, ovsdbConnectionInstance);

        if (ovsdbConnectionInstance == null) {
            LOG.warn("handleOwnershipChanged: found no connection instance for {}", ownershipChange.getEntity());
            return;
        }

        if (ownershipChange.isOwner() == ovsdbConnectionInstance.getHasDeviceOwnership()) {
            LOG.debug("handleOwnershipChanged: no local changes for {}. Owner is: {}",
                    ovsdbConnectionInstance.getNodeId().getValue(), ovsdbConnectionInstance.getHasDeviceOwnership());
            return;
        }

        ovsdbConnectionInstance.setHasDeviceOwnership(ownershipChange.isOwner());

        if (ownershipChange.isOwner()) {
            LOG.info("handleOwnershipChanged: connection instance for {} is owner",
                    ovsdbConnectionInstance.getNodeId().getValue());

            putConnectionInstance(ovsdbConnectionInstance.getMDConnectionInfo(),
                    ovsdbConnectionInstance); // from connectedButCallBacksNotRegistered
            ovsdbConnectionInstance.createTransactInvokers(); // from connectedButCallBacksNotRegistered
            ovsdbConnectionInstance.registerCallbacks(); // from connected

        } else {
            // NOTE: This should never happen!
            LOG.error("handleOwnershipChanged: connection instance for {} is no longer the owner",
                    ovsdbConnectionInstance.getNodeId().getValue());
            // TODO: reverse all the things we did in the block above?!?
        }
    }

    private Entity getEntityFromConnectionInstance(@Nonnull OvsdbConnectionInstance ovsdbConnectionInstance) {
        YangInstanceIdentifier entityId = SouthboundUtil.getInstanceIdentifierCodec()
                .getYangInstanceIdentifier(ovsdbConnectionInstance.getInstanceIdentifier());
        return new Entity(ENTITY_TYPE, entityId);
    }

    private OvsdbConnectionInstance getConnectionInstanceFromEntity(Entity entity) {
        InstanceIdentifier<Node> nodeId = (InstanceIdentifier<Node>) SouthboundUtil.getInstanceIdentifierCodec()
                .bindingDeserializer(entity.getId());
        return candidateConnectionIidMap.get(nodeId);
    }

    private class OvsdbDeviceEntityOwnershipListener implements EntityOwnershipListener {
        private OvsdbConnectionManager cm;

        OvsdbDeviceEntityOwnershipListener(OvsdbConnectionManager cm, EntityOwnershipService entityOwnershipService) {
            this.cm = cm;
            entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            cm.handleOwnershipChanged(ownershipChange);
        }
    }
}
