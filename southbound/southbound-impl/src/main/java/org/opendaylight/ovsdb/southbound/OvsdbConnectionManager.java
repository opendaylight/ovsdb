/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbNodeRemoveCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    private Map<ConnectionInfo, OvsdbConnectionInstance> clients =
            new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);
    private static final String ENTITY_TYPE = "ovsdb";

    private DataBroker db;
    private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers =
            new ConcurrentHashMap<>();
    private Map<Entity, OvsdbConnectionInstance> entityConnectionMap =
            new ConcurrentHashMap<>();
    private EntityOwnershipService entityOwnershipService;
    private OvsdbDeviceEntityOwnershipListener ovsdbDeviceEntityOwnershipListener;
    private OvsdbConnection ovsdbConnection;

    public OvsdbConnectionManager(DataBroker db,TransactionInvoker txInvoker,
                                  EntityOwnershipService entityOwnershipService,
                                  OvsdbConnection ovsdbConnection) {
        this.db = db;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        this.ovsdbDeviceEntityOwnershipListener = new OvsdbDeviceEntityOwnershipListener(this, entityOwnershipService);
        this.ovsdbConnection = ovsdbConnection;
    }

    @Override
    public void connected(@Nonnull final OvsdbClient externalClient) {
        LOG.info("Library connected {} from {}:{} to {}:{}",
                externalClient.getConnectionInfo().getType(),
                externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort(),
                externalClient.getConnectionInfo().getLocalAddress(),
                externalClient.getConnectionInfo().getLocalPort());
        OvsdbConnectionInstance client = connectedButCallBacksNotRegistered(externalClient);

        // Register Cluster Ownership for ConnectionInfo
        registerEntityForOwnership(client);
    }

    public OvsdbConnectionInstance connectedButCallBacksNotRegistered(final OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        ConnectionInfo key = SouthboundMapper.createConnectionInfo(externalClient);
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(key);

        // Check if existing ovsdbConnectionInstance for the OvsdbClient present.
        // In such cases, we will see if the ovsdbConnectionInstance has same externalClient.
        if (ovsdbConnectionInstance != null) {
            if (ovsdbConnectionInstance.hasOvsdbClient(externalClient)) {
                LOG.warn("OVSDB Connection Instance {} already exists for client {}", key, externalClient);
                return ovsdbConnectionInstance;
            }
            LOG.warn("OVSDB Connection Instance {} being replaced with client {}", key, externalClient);

            // Unregister Cluster Ownership for ConnectionInfo
            // Because the ovsdbConnectionInstance is about to be completely replaced!
            unregisterEntityForOwnership(ovsdbConnectionInstance);

            ovsdbConnectionInstance.disconnect();

            removeConnectionInstance(key);
        }

        ovsdbConnectionInstance = new OvsdbConnectionInstance(key, externalClient, txInvoker,
                getInstanceIdentifier(key));
        ovsdbConnectionInstance.createTransactInvokers();
        return ovsdbConnectionInstance;
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("Library disconnected {} from {}:{} to {}:{}. Cleaning up the operational data store",
                client.getConnectionInfo().getType(),
                client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort(),
                client.getConnectionInfo().getLocalAddress(),
                client.getConnectionInfo().getLocalPort());
        ConnectionInfo key = SouthboundMapper.createConnectionInfo(client);
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(key);
        if (ovsdbConnectionInstance != null) {
            // Unregister Entity ownership as soon as possible ,so this instance should
            // not be used as a candidate in Entity election (given that this instance is
            // about to disconnect as well), if current owner get disconnected from
            // OVSDB device.
            unregisterEntityForOwnership(ovsdbConnectionInstance);

            txInvoker.invoke(new OvsdbNodeRemoveCommand(ovsdbConnectionInstance, null, null));

            removeConnectionInstance(key);
        } else {
            LOG.warn("disconnected : Connection instance not found for OVSDB Node {} ", key);
        }
        LOG.trace("OvsdbConnectionManager: exit disconnected client: {}", client);
    }

    public OvsdbClient connect(InstanceIdentifier<Node> iid,
            OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        LOG.info("Connecting to {}", SouthboundUtil.connectionInfoToString(ovsdbNode.getConnectionInfo()));

        // TODO handle case where we already have a connection
        // TODO use transaction chains to handle ordering issues between disconnected
        // TODO and connected when writing to the operational store
        InetAddress ip = SouthboundMapper.createInetAddress(ovsdbNode.getConnectionInfo().getRemoteIp());
        OvsdbClient client = ovsdbConnection.connect(ip,
                ovsdbNode.getConnectionInfo().getRemotePort().getValue());
        // For connections from the controller to the ovs instance, the library doesn't call
        // this method for us
        if (client != null) {
            putInstanceIdentifier(ovsdbNode.getConnectionInfo(), iid.firstIdentifierOf(Node.class));
            OvsdbConnectionInstance ovsdbConnectionInstance = connectedButCallBacksNotRegistered(client);
            ovsdbConnectionInstance.setOvsdbNodeAugmentation(ovsdbNode);

            // Register Cluster Ownership for ConnectionInfo
            registerEntityForOwnership(ovsdbConnectionInstance);
        } else {
            LOG.warn("Failed to connect to OVSDB Node {}", ovsdbNode.getConnectionInfo());
        }
        return client;
    }

    public void disconnect(OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        LOG.info("Disconnecting from {}", SouthboundUtil.connectionInfoToString(ovsdbNode.getConnectionInfo()));
        OvsdbConnectionInstance client = getConnectionInstance(ovsdbNode.getConnectionInfo());
        if (client != null) {
            // Unregister Cluster Onwership for ConnectionInfo
            unregisterEntityForOwnership(client);

            client.disconnect();

            removeInstanceIdentifier(ovsdbNode.getConnectionInfo());
        } else {
            LOG.debug("disconnect : connection instance not found for {}",ovsdbNode.getConnectionInfo());
        }
    }

/*    public void init(ConnectionInfo key) {
        OvsdbConnectionInstance client = getConnectionInstance(key);

        // TODO (FF): make sure that this cluster instance is the 'entity owner' fo the given OvsdbConnectionInstance ?

        if (client != null) {

             *  Note: registerCallbacks() is idemPotent... so if you call it repeatedly all is safe,
             *  it only registersCallbacks on the *first* call.

            client.registerCallbacks();
        }
    }
*/
    @Override
    public void close() throws Exception {
        if (ovsdbDeviceEntityOwnershipListener != null) {
            ovsdbDeviceEntityOwnershipListener.close();
        }

        for (OvsdbClient client: clients.values()) {
            client.disconnect();
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
        return instanceIdentifiers.get(connectionInfo);
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
            if (optional != null && optional.isPresent() && optional.get() != null) {
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

    private void handleOwnershipChanged(EntityOwnershipChange ownershipChange) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstanceFromEntity(ownershipChange.getEntity());
        LOG.debug("handleOwnershipChanged: {} event received for device {}",
                ownershipChange, ovsdbConnectionInstance != null ? ovsdbConnectionInstance.getConnectionInfo()
                        : "that's currently NOT registered by *this* southbound plugin instance");

        if (ovsdbConnectionInstance == null) {
            if (ownershipChange.isOwner()) {
                LOG.warn("handleOwnershipChanged: *this* instance is elected as an owner of the device {} but it "
                        + "is NOT registered for ownership", ownershipChange.getEntity());
            } else {
                // EntityOwnershipService sends notification to all the nodes, irrespective of whether
                // that instance registered for the device ownership or not. It is to make sure that
                // If all the controller instance that was connected to the device are down, so the
                // running instance can clear up the operational data store even though it was not
                // connected to the device.
                LOG.debug("handleOwnershipChanged: No connection instance found for {}", ownershipChange.getEntity());
            }

            // If entity has no owner, clean up the operational data store (it's possible because owner controller
            // might went down abruptly and didn't get a chance to clean up the operational data store.
            if (!ownershipChange.hasOwner()) {
                LOG.info("{} has no owner, cleaning up the operational data store", ownershipChange.getEntity());
                cleanEntityOperationalData(ownershipChange.getEntity());
            }
            return;
        }
        //Connection detail need to be cached, irrespective of ownership result.
        putConnectionInstance(ovsdbConnectionInstance.getMDConnectionInfo(),ovsdbConnectionInstance);

        if (ownershipChange.isOwner() == ovsdbConnectionInstance.getHasDeviceOwnership()) {
            LOG.info("handleOwnershipChanged: no change in ownership for {}. Ownership status is : {}",
                    ovsdbConnectionInstance.getConnectionInfo(), ovsdbConnectionInstance.getHasDeviceOwnership()
                            ? SouthboundConstants.OWNERSHIPSTATES.OWNER.getState()
                            : SouthboundConstants.OWNERSHIPSTATES.NONOWNER.getState());
            return;
        }

        ovsdbConnectionInstance.setHasDeviceOwnership(ownershipChange.isOwner());
        // You were not an owner, but now you are
        if (ownershipChange.isOwner()) {
            LOG.info("handleOwnershipChanged: *this* southbound plugin instance is an OWNER of the device {}",
                    ovsdbConnectionInstance.getConnectionInfo());

            //*this* instance of southbound plugin is owner of the device,
            //so register for monitor callbacks
            ovsdbConnectionInstance.registerCallbacks();

        } else {
            //You were owner of the device, but now you are not. With the current ownership
            //grant mechanism, this scenario should not occur. Because this scenario will occur
            //when this controller went down or switch flap the connection, but in both the case
            //it will go through the re-registration process. We need to implement this condition
            //when clustering service implement a ownership grant strategy which can revoke the
            //device ownership for load balancing the devices across the instances.
            //Once this condition occur, we should unregister the callback.
            LOG.error("handleOwnershipChanged: *this* southbound plugin instance is no longer the owner of device {}."
                    + "This should NOT happen.",
                    ovsdbConnectionInstance.getNodeId().getValue());
        }
    }

    private void cleanEntityOperationalData(Entity entity) {

        //Do explicit cleanup rather then using OvsdbNodeRemoveCommand, because there
        // are chances that other controller instance went down abruptly and it does
        // not clear manager entry, which OvsdbNodeRemoveCommand look for before cleanup.

        InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>) SouthboundUtil
                .getInstanceIdentifierCodec().bindingDeserializer(entity.getId());

        final ReadWriteTransaction transaction = db.newReadWriteTransaction();
        Optional<Node> ovsdbNodeOpt = SouthboundUtil.readNode(transaction,nodeIid);
        if ( ovsdbNodeOpt.isPresent() ) {
            Node ovsdbNode = ovsdbNodeOpt.get();
            OvsdbNodeAugmentation nodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
            LOG.info("ANIL VISHNOI : nodeAugmentation : {}",nodeAugmentation);
            if (nodeAugmentation != null) {
                if (nodeAugmentation.getManagedNodeEntry() != null) {
                    for (ManagedNodeEntry managedNode : nodeAugmentation.getManagedNodeEntry()) {
                        transaction.delete(
                                LogicalDatastoreType.OPERATIONAL, managedNode.getBridgeRef().getValue());
                    }
                } else {
                    LOG.debug("{} had no managed nodes", ovsdbNode.getNodeId().getValue());
                }
            }
            SouthboundUtil.deleteNode(transaction, nodeIid);
        }
    }

    private OpenVSwitch getOpenVswitchTableEntry(OvsdbConnectionInstance connectionInstance) {
        DatabaseSchema dbSchema = null;
        OpenVSwitch openVSwitchRow = null;
        try {
            dbSchema = connectionInstance.getSchema(OvsdbSchemaContants.databaseName).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    OvsdbSchemaContants.databaseName,connectionInstance.getConnectionInfo(),e);
        }
        if (dbSchema != null) {
            GenericTableSchema openVSwitchSchema = TyperUtils.getTableSchema(dbSchema, OpenVSwitch.class);

            List<String> openVSwitchTableColumn = new ArrayList<>();
            openVSwitchTableColumn.addAll(openVSwitchSchema.getColumns());
            Select<GenericTableSchema> selectOperation = op.select(openVSwitchSchema);
            selectOperation.setColumns(openVSwitchTableColumn);

            List<Operation> operations = new ArrayList<>();
            operations.add(selectOperation);
            operations.add(op.comment("Fetching Open_VSwitch table rows"));
            try {
                List<OperationResult> results = connectionInstance.transact(dbSchema, operations).get();
                if (results != null ) {
                    OperationResult selectResult = results.get(0);
                    openVSwitchRow = TyperUtils.getTypedRowWrapper(
                            dbSchema,OpenVSwitch.class,selectResult.getRows().get(0));

                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Not able to fetch OpenVswitch table row from device {}",
                        connectionInstance.getConnectionInfo(),e);
            }
        }
        return openVSwitchRow;
    }
    private Entity getEntityFromConnectionInstance(@Nonnull OvsdbConnectionInstance ovsdbConnectionInstance) {
        InstanceIdentifier<Node> iid = ovsdbConnectionInstance.getInstanceIdentifier();
        if ( iid == null ) {
            /* Switch initiated connection won't have iid, till it gets OpenVSwitch
             * table update but update callback is always registered after ownership
             * is granted. So we are explicitly fetch the row here to get the iid.
             */
            OpenVSwitch openvswitchRow = getOpenVswitchTableEntry(ovsdbConnectionInstance);
            iid = SouthboundMapper.getInstanceIdentifier(openvswitchRow);
            LOG.info("InstanceIdentifier {} generated for device "
                    + "connection {}",iid,ovsdbConnectionInstance.getConnectionInfo());
            ovsdbConnectionInstance.setInstanceIdentifier(iid);
        }
        YangInstanceIdentifier entityId = SouthboundUtil.getInstanceIdentifierCodec().getYangInstanceIdentifier(iid);
        Entity deviceEntity = new Entity(ENTITY_TYPE, entityId);
        LOG.debug("Entity {} created for device connection {}",
                deviceEntity, ovsdbConnectionInstance.getConnectionInfo());
        return deviceEntity;
    }

    private OvsdbConnectionInstance getConnectionInstanceFromEntity(Entity entity) {
        return entityConnectionMap.get(entity);
    }

    private void registerEntityForOwnership(OvsdbConnectionInstance ovsdbConnectionInstance) {

        Entity candidateEntity = getEntityFromConnectionInstance(ovsdbConnectionInstance);
        entityConnectionMap.put(candidateEntity, ovsdbConnectionInstance);
        ovsdbConnectionInstance.setConnectedEntity(candidateEntity);
        try {
            EntityOwnershipCandidateRegistration registration =
                    entityOwnershipService.registerCandidate(candidateEntity);
            ovsdbConnectionInstance.setDeviceOwnershipCandidateRegistration(registration);
            LOG.info("OVSDB entity {} is registered for ownership.", candidateEntity);

            //If entity already has owner, it won't get notification from EntityOwnershipService
            //so cache the connection instances.
            Optional<EntityOwnershipState> ownershipStateOpt =
                    entityOwnershipService.getOwnershipState(candidateEntity);
            if (ownershipStateOpt.isPresent()) {
                EntityOwnershipState ownershipState = ownershipStateOpt.get();
                if (ownershipState.hasOwner() && !ownershipState.isOwner()) {
                    LOG.info("OVSDB entity {} is already owned by other southbound plugin "
                                    + "instance, so *this* instance is NOT an OWNER of the device",
                            ovsdbConnectionInstance.getConnectionInfo());
                    putConnectionInstance(ovsdbConnectionInstance.getMDConnectionInfo(),ovsdbConnectionInstance);
                }
            }
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB entity {} was already registered for ownership", candidateEntity, e);
        }

    }

    private void unregisterEntityForOwnership(OvsdbConnectionInstance ovsdbConnectionInstance) {
        ovsdbConnectionInstance.closeDeviceOwnershipCandidateRegistration();
        entityConnectionMap.remove(ovsdbConnectionInstance.getConnectedEntity());
    }

    private class OvsdbDeviceEntityOwnershipListener implements EntityOwnershipListener {
        private OvsdbConnectionManager cm;
        private EntityOwnershipListenerRegistration listenerRegistration;

        OvsdbDeviceEntityOwnershipListener(OvsdbConnectionManager cm, EntityOwnershipService entityOwnershipService) {
            this.cm = cm;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }
        public void close() {
            listenerRegistration.close();
        }
        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            cm.handleOwnershipChanged(ownershipChange);
        }
    }
}
