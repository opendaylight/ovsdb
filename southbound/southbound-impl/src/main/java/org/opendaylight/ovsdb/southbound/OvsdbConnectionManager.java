/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.southbound.reconciliation.configuration.BridgeConfigReconciliationTask;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbNodeRemoveCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionCommand;
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

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    private Map<ConnectionInfo, OvsdbConnectionInstance> clients =
            new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);
    private static final String ENTITY_TYPE = "ovsdb";
    private static final int DB_FETCH_TIMEOUT = 1000;

    private DataBroker db;
    private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers =
            new ConcurrentHashMap<>();
    private Map<Entity, OvsdbConnectionInstance> entityConnectionMap =
            new ConcurrentHashMap<>();
    private EntityOwnershipService entityOwnershipService;
    private OvsdbDeviceEntityOwnershipListener ovsdbDeviceEntityOwnershipListener;
    private OvsdbConnection ovsdbConnection;
    private final ReconciliationManager reconciliationManager;
    private final InstanceIdentifierCodec instanceIdentifierCodec;

    public OvsdbConnectionManager(DataBroker db,TransactionInvoker txInvoker,
                                  EntityOwnershipService entityOwnershipService,
                                  OvsdbConnection ovsdbConnection,
                                  InstanceIdentifierCodec instanceIdentifierCodec) {
        this.db = db;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        this.ovsdbDeviceEntityOwnershipListener = new OvsdbDeviceEntityOwnershipListener(this, entityOwnershipService);
        this.ovsdbConnection = ovsdbConnection;
        this.reconciliationManager = new ReconciliationManager(db, instanceIdentifierCodec);
        this.instanceIdentifierCodec = instanceIdentifierCodec;
    }

    @Override
    public void connected(@Nonnull final OvsdbClient externalClient) {
        LOG.info("Library connected {} from {}:{} to {}:{}",
                externalClient.getConnectionInfo().getType(),
                externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort(),
                externalClient.getConnectionInfo().getLocalAddress(),
                externalClient.getConnectionInfo().getLocalPort());
        List<String> databases = new ArrayList<>();
        try {
            databases = externalClient.getDatabases().get(DB_FETCH_TIMEOUT, TimeUnit.MILLISECONDS);
            if (databases.contains(SouthboundConstants.OPEN_V_SWITCH)) {
                OvsdbConnectionInstance client = connectedButCallBacksNotRegistered(externalClient);
                // Register Cluster Ownership for ConnectionInfo
                registerEntityForOwnership(client);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to fetch Database list from device {}. Disconnecting from the device.",
                    externalClient.getConnectionInfo().getRemoteAddress(), e);
            externalClient.disconnect();
        }

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

            stopBridgeConfigReconciliationIfActive(ovsdbConnectionInstance.getInstanceIdentifier());
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

            //Controller initiated connection can be terminated from switch side.
            //So cleanup the instance identifier cache.
            removeInstanceIdentifier(key);
            stopBridgeConfigReconciliationIfActive(ovsdbConnectionInstance.getInstanceIdentifier());
            retryConnection(ovsdbConnectionInstance.getInstanceIdentifier(),
                    ovsdbConnectionInstance.getOvsdbNodeAugmentation(),
                    ConnectionReconciliationTriggers.ON_DISCONNECT);
        } else {
            LOG.warn("disconnected : Connection instance not found for OVSDB Node {} ", key);
        }
        LOG.trace("OvsdbConnectionManager: exit disconnected client: {}", client);
    }

    public OvsdbClient connect(InstanceIdentifier<Node> iid,
            OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException, ConnectException {
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

            stopBridgeConfigReconciliationIfActive(client.getInstanceIdentifier());
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
    public void close() {
        if (ovsdbDeviceEntityOwnershipListener != null) {
            ovsdbDeviceEntityOwnershipListener.close();
        }

        for (OvsdbConnectionInstance client: clients.values()) {
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

    public InstanceIdentifier<Node> getInstanceIdentifier(ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        return instanceIdentifiers.get(connectionInfo);
    }

    public OvsdbConnectionInstance getConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
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
            if (optional.isPresent()) {
                return this.getConnectionInstance(optional.get());
            } else {
                LOG.debug("Node was not found on the path in the operational DS: {}", nodePath);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to get Ovsdb Node {}",nodePath, e);
            return null;
        }
    }

    public OvsdbClient getClient(ConnectionInfo connectionInfo) {
        OvsdbConnectionInstance connectionInstance = getConnectionInstance(connectionInfo);
        if (connectionInstance != null) {
            return connectionInstance.getOvsdbClient();
        }
        return null;
    }

    public OvsdbClient getClient(OvsdbBridgeAttributes mn) {
        return getConnectionInstance(mn).getOvsdbClient();
    }

    public OvsdbClient getClient(Node node) {
        return getConnectionInstance(node).getOvsdbClient();
    }

    public Boolean getHasDeviceOwnership(ConnectionInfo connectionInfo) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(connectionInfo);
        if (ovsdbConnectionInstance == null) {
            return Boolean.FALSE;
        }
        return ovsdbConnectionInstance.getHasDeviceOwnership();
    }

    public void reconcileConnection(InstanceIdentifier<Node> iid, OvsdbNodeAugmentation ovsdbNode) {
        this.retryConnection(iid, ovsdbNode,
                ConnectionReconciliationTriggers.ON_CONTROLLER_INITIATED_CONNECTION_FAILURE);

    }

    public void stopConnectionReconciliationIfActive(InstanceIdentifier<?> iid, OvsdbNodeAugmentation ovsdbNode) {
        final ReconciliationTask task = new ConnectionReconciliationTask(
                reconciliationManager,
                this,
                iid,
                ovsdbNode);
        reconciliationManager.dequeue(task);
    }

    public void stopBridgeConfigReconciliationIfActive(InstanceIdentifier<?> iid) {
        final ReconciliationTask task =
                new BridgeConfigReconciliationTask(reconciliationManager, this, iid, null, instanceIdentifierCodec);
        reconciliationManager.dequeue(task);
        reconciliationManager.cancelTerminationPointReconciliation();
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
                            ? SouthboundConstants.OwnershipStates.OWNER.getState()
                            : SouthboundConstants.OwnershipStates.NONOWNER.getState());
            return;
        }

        ovsdbConnectionInstance.setHasDeviceOwnership(ownershipChange.isOwner());
        // You were not an owner, but now you are
        if (ownershipChange.isOwner()) {
            LOG.info("handleOwnershipChanged: *this* southbound plugin instance is an OWNER of the device {}",
                    ovsdbConnectionInstance.getConnectionInfo());

            //*this* instance of southbound plugin is owner of the device,
            //so register for monitor callbacks
            ovsdbConnectionInstance.registerCallbacks(instanceIdentifierCodec);

            reconcileBridgeConfigurations(ovsdbConnectionInstance);
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

        //Do explicit cleanup rather than using OvsdbNodeRemoveCommand, because there
        // are chances that other controller instance went down abruptly and it does
        // not clear manager entry, which OvsdbNodeRemoveCommand look for before cleanup.

        @SuppressWarnings("unchecked") final InstanceIdentifier<Node> nodeIid =
                (InstanceIdentifier<Node>) instanceIdentifierCodec.bindingDeserializer(entity.getId());

        txInvoker.invoke(new TransactionCommand() {
            @Override
            public void execute(ReadWriteTransaction transaction) {
                Optional<Node> ovsdbNodeOpt = SouthboundUtil.readNode(transaction, nodeIid);
                if (ovsdbNodeOpt.isPresent()) {
                    Node ovsdbNode = ovsdbNodeOpt.get();
                    OvsdbNodeAugmentation nodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
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
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeIid);
                }
            }
        });

    }

    private OpenVSwitch getOpenVswitchTableEntry(OvsdbConnectionInstance connectionInstance) {
        DatabaseSchema dbSchema = null;
        OpenVSwitch openVSwitchRow = null;
        try {
            dbSchema = connectionInstance.getSchema(OvsdbSchemaContants.DATABASE_NAME).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    OvsdbSchemaContants.DATABASE_NAME,connectionInstance.getConnectionInfo(),e);
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
                if (results != null) {
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
        if (iid == null) {
            /* Switch initiated connection won't have iid, till it gets OpenVSwitch
             * table update but update callback is always registered after ownership
             * is granted. So we are explicitly fetch the row here to get the iid.
             */
            OpenVSwitch openvswitchRow = getOpenVswitchTableEntry(ovsdbConnectionInstance);
            iid = SouthboundMapper.getInstanceIdentifier(instanceIdentifierCodec, openvswitchRow);
            LOG.info("InstanceIdentifier {} generated for device "
                    + "connection {}",iid,ovsdbConnectionInstance.getConnectionInfo());
            ovsdbConnectionInstance.setInstanceIdentifier(iid);
        }
        YangInstanceIdentifier entityId = instanceIdentifierCodec.getYangInstanceIdentifier(iid);
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

    private void retryConnection(final InstanceIdentifier<Node> iid, final OvsdbNodeAugmentation ovsdbNode,
                                 ConnectionReconciliationTriggers trigger) {
        final ReconciliationTask task = new ConnectionReconciliationTask(
                reconciliationManager,
                this,
                iid,
                ovsdbNode);

        if (reconciliationManager.isEnqueued(task)) {
            return;
        }
        switch (trigger) {
            case ON_CONTROLLER_INITIATED_CONNECTION_FAILURE:
                reconciliationManager.enqueueForRetry(task);
                break;
            case ON_DISCONNECT:
            {
                ReadOnlyTransaction tx = db.newReadOnlyTransaction();
                CheckedFuture<Optional<Node>, ReadFailedException> readNodeFuture =
                        tx.read(LogicalDatastoreType.CONFIGURATION, iid);

                final OvsdbConnectionManager connectionManager = this;
                Futures.addCallback(readNodeFuture, new FutureCallback<Optional<Node>>() {
                    @Override
                    public void onSuccess(@Nullable Optional<Node> node) {
                        if (node.isPresent()) {
                            LOG.info("Disconnected/Failed connection {} was controller initiated, attempting "
                                    + "reconnection", ovsdbNode.getConnectionInfo());
                            reconciliationManager.enqueue(task);

                        } else {
                            LOG.debug("Connection {} was switch initiated, no reconciliation is required",
                                    iid.firstKeyOf(Node.class).getNodeId());
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.warn("Read Config/DS for Node failed! {}", iid, throwable);
                    }
                });
                break;
            }
            default:
                break;
        }
    }

    private void reconcileBridgeConfigurations(final OvsdbConnectionInstance client) {
        final InstanceIdentifier<Node> nodeIid = client.getInstanceIdentifier();
        final ReconciliationTask task = new BridgeConfigReconciliationTask(
                reconciliationManager, OvsdbConnectionManager.this, nodeIid, client, instanceIdentifierCodec);

        reconciliationManager.enqueue(task);
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

    private enum ConnectionReconciliationTriggers {
        /*
        Reconciliation trigger for scenario where controller's attempt
        to connect to switch fails on config data store notification
        */
        ON_CONTROLLER_INITIATED_CONNECTION_FAILURE,

        /*
        Reconciliation trigger for the scenario where controller
        initiated connection disconnects.
        */
        ON_DISCONNECT
    }
}
