/*
 * Copyright © 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipStateChange;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.southbound.reconciliation.configuration.BridgeConfigReconciliationTask;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbNodeRemoveCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);
    private static final String ENTITY_TYPE = "ovsdb";
    private static final int DB_FETCH_TIMEOUT = 1000;

    private final ConcurrentMap<ConnectionInfo, OvsdbConnectionInstance> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<OvsdbClient, OvsdbClient> alreadyProcessedClients = new ConcurrentHashMap<>();
    private final ConcurrentMap<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<InstanceIdentifier<Node>, OvsdbConnectionInstance> nodeIdVsConnectionInstance =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<Entity, OvsdbConnectionInstance> entityConnectionMap = new ConcurrentHashMap<>();
    private final DataBroker db;
    private final Operations ops;
    private final TransactionInvoker txInvoker;
    private final EntityOwnershipService entityOwnershipService;
    private final OvsdbDeviceEntityOwnershipListener ovsdbDeviceEntityOwnershipListener;
    private final OvsdbConnection ovsdbConnection;
    private final ReconciliationManager reconciliationManager;
    private final InstanceIdentifierCodec instanceIdentifierCodec;

    public OvsdbConnectionManager(final DataBroker db, final Operations ops, final TransactionInvoker txInvoker,
                                  final EntityOwnershipService entityOwnershipService,
                                  final OvsdbConnection ovsdbConnection,
                                  final InstanceIdentifierCodec instanceIdentifierCodec,
                                  final List<String> reconcileBridgeInclusionList,
                                  final List<String> reconcileBridgeExclusionList) {
        this.db = db;
        this.ops = ops;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        ovsdbDeviceEntityOwnershipListener = new OvsdbDeviceEntityOwnershipListener(this, entityOwnershipService);
        this.ovsdbConnection = ovsdbConnection;
        reconciliationManager = new ReconciliationManager(db, instanceIdentifierCodec,
            reconcileBridgeInclusionList, reconcileBridgeExclusionList);
        this.instanceIdentifierCodec = instanceIdentifierCodec;
    }

    @Override
    public void connected(final OvsdbClient externalClient) {
        if (alreadyProcessedClients.containsKey(externalClient)) {
            LOG.info("OvsdbConnectionManager Library already connected {} from {}:{} to {}:{} "
                            + "to this, hence skipping the processing",
                    externalClient.getConnectionInfo().getType(),
                    externalClient.getConnectionInfo().getRemoteAddress(),
                    externalClient.getConnectionInfo().getRemotePort(),
                    externalClient.getConnectionInfo().getLocalAddress(),
                    externalClient.getConnectionInfo().getLocalPort());
            return;
        }
        alreadyProcessedClients.put(externalClient, externalClient);

        LOG.info("OvsdbConnectionManager connected {} from {}:{} to {}:{}",
                externalClient.getConnectionInfo().getType(),
                externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort(),
                externalClient.getConnectionInfo().getLocalAddress(),
                externalClient.getConnectionInfo().getLocalPort());
        try {
            List<String> databases = externalClient.getDatabases().get(DB_FETCH_TIMEOUT, TimeUnit.MILLISECONDS);
            if (databases.contains(SouthboundConstants.OPEN_V_SWITCH)) {
                OvsdbConnectionInstance client = connectedButCallBacksNotRegistered(externalClient);
                // Register Cluster Ownership for ConnectionInfo
                registerEntityForOwnership(client);
                OvsdbOperGlobalListener.runAfterTimeoutIfNodeNotCreated(client.getInstanceIdentifier(), () -> {
                    externalClient.disconnect();
                    disconnected(externalClient);
                });
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("OvsdbConnectionManager Unable to fetch Database list from device {}."
                    + "Disconnecting from the device.", externalClient.getConnectionInfo().getRemoteAddress(), e);
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

        ovsdbConnectionInstance = new OvsdbConnectionInstance(key, externalClient, ops, txInvoker,
                getInstanceIdentifier(key));
        ovsdbConnectionInstance.createTransactInvokers();
        return ovsdbConnectionInstance;
    }

    @Override
    public void disconnected(final OvsdbClient client) {
        alreadyProcessedClients.remove(client);
        LOG.info("Ovsdb Library disconnected {} from {}:{} to {}:{}. Cleaning up the operational data store",
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
            if (ovsdbConnectionInstance.getHasDeviceOwnership()) {
                LOG.info("Ovsdb Library disconnected {} this controller instance has ownership", key);
                deleteOperNodeAndReleaseOwnership(ovsdbConnectionInstance);
            } else {
                LOG.info("Ovsdb Library disconnected {} this controller does not have ownership", key);
                unregisterEntityForOwnership(ovsdbConnectionInstance);
            }
            removeConnectionInstance(key);

            //Controller initiated connection can be terminated from switch side.
            //So cleanup the instance identifier cache.
            removeInstanceIdentifier(key);
            nodeIdVsConnectionInstance.remove(ovsdbConnectionInstance.getInstanceIdentifier(),
                    ovsdbConnectionInstance);
            stopBridgeConfigReconciliationIfActive(ovsdbConnectionInstance.getInstanceIdentifier());
            retryConnection(ovsdbConnectionInstance.getInstanceIdentifier(),
                    ovsdbConnectionInstance.getOvsdbNodeAugmentation(),
                    ConnectionReconciliationTriggers.ON_DISCONNECT);
        } else {
            LOG.warn("Ovsdb disconnected : Connection instance not found for OVSDB Node {} ", key);
        }
        LOG.trace("OvsdbConnectionManager: exit disconnected client: {}", client);
    }

    private void deleteOperNodeAndReleaseOwnership(final OvsdbConnectionInstance ovsdbConnectionInstance) {
        ovsdbConnectionInstance.setHasDeviceOwnership(false);
        final InstanceIdentifier<?> nodeIid = ovsdbConnectionInstance.getInstanceIdentifier();
        //remove the node from oper only if it has ownership
        txInvoker.invoke(new OvsdbNodeRemoveCommand(ovsdbConnectionInstance, null, null) {

            @Override
            public void onSuccess() {
                super.onSuccess();
                LOG.debug("Successfully removed node {} from oper", nodeIid);
                //Giveup the ownership only after cleanup is done
                unregisterEntityForOwnership(ovsdbConnectionInstance);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.debug("Failed to remove node {} from oper", nodeIid);
                super.onFailure(throwable);
                unregisterEntityForOwnership(ovsdbConnectionInstance);
            }
        });
    }

    public OvsdbClient connect(final InstanceIdentifier<Node> iid,
            final OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException, ConnectException {
        LOG.info("Connecting to {}", SouthboundUtil.connectionInfoToString(ovsdbNode.getConnectionInfo()));

        // TODO handle case where we already have a connection
        // TODO use transaction chains to handle ordering issues between disconnected
        // TODO and connected when writing to the operational store
        InetAddress ip = SouthboundMapper.createInetAddress(ovsdbNode.getConnectionInfo().getRemoteIp());
        OvsdbClient client = ovsdbConnection.connect(ip,
                ovsdbNode.getConnectionInfo().getRemotePort().getValue().toJava());
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

    public void disconnect(final OvsdbNodeAugmentation ovsdbNode) throws UnknownHostException {
        LOG.info("Disconnecting from {}", SouthboundUtil.connectionInfoToString(ovsdbNode.getConnectionInfo()));
        OvsdbConnectionInstance client = getConnectionInstance(ovsdbNode.getConnectionInfo());
        if (client != null) {
            // Unregister Cluster Onwership for ConnectionInfo
            deleteOperNodeAndReleaseOwnership(client);

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

    @VisibleForTesting
    void putConnectionInstance(final ConnectionInfo key,final OvsdbConnectionInstance instance) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
    }

    private void removeConnectionInstance(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        clients.remove(connectionInfo);
    }

    @VisibleForTesting
    void putInstanceIdentifier(final ConnectionInfo key, final InstanceIdentifier<Node> iid) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.put(connectionInfo, iid);
    }

    private void removeInstanceIdentifier(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.remove(connectionInfo);
    }

    public InstanceIdentifier<Node> getInstanceIdentifier(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        return instanceIdentifiers.get(connectionInfo);
    }

    public OvsdbConnectionInstance getConnectionInstance(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = SouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
    }

    public OvsdbConnectionInstance getConnectionInstance(final OvsdbBridgeAttributes mn) {
        Optional<OvsdbNodeAugmentation> optional = SouthboundUtil.getManagingNode(db, mn);
        if (optional.isPresent()) {
            return getConnectionInstance(optional.orElseThrow().getConnectionInfo());
        } else {
            return null;
        }
    }

    public OvsdbConnectionInstance getConnectionInstance(final Node node) {
        requireNonNull(node);
        OvsdbNodeAugmentation ovsdbNode = node.augmentation(OvsdbNodeAugmentation.class);
        OvsdbBridgeAugmentation ovsdbManagedNode = node.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNode != null) {
            return getConnectionInstance(ovsdbNode.getConnectionInfo());
        } else if (ovsdbManagedNode != null) {
            return getConnectionInstance(ovsdbManagedNode);
        } else {
            LOG.warn("This is not a node that gives any hint how to find its OVSDB Manager: {}",node);
            return null;
        }
    }

    public OvsdbConnectionInstance getConnectionInstance(final InstanceIdentifier<Node> nodePath) {
        if (nodeIdVsConnectionInstance.get(nodePath) != null) {
            return nodeIdVsConnectionInstance.get(nodePath);
        }
        try {
            ReadTransaction transaction = db.newReadOnlyTransaction();
            FluentFuture<Optional<Node>> nodeFuture = transaction.read(
                    LogicalDatastoreType.OPERATIONAL, nodePath);
            transaction.close();
            Optional<Node> optional = nodeFuture.get();
            if (optional.isPresent()) {
                return this.getConnectionInstance(optional.orElseThrow());
            } else {
                LOG.debug("Node was not found on the path in the operational DS: {}", nodePath);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to get Ovsdb Node {}",nodePath, e);
            return null;
        }
    }

    public OvsdbClient getClient(final ConnectionInfo connectionInfo) {
        OvsdbConnectionInstance connectionInstance = getConnectionInstance(connectionInfo);
        if (connectionInstance != null) {
            return connectionInstance.getOvsdbClient();
        }
        return null;
    }

    public OvsdbClient getClient(final OvsdbBridgeAttributes mn) {
        return getConnectionInstance(mn).getOvsdbClient();
    }

    public OvsdbClient getClient(final Node node) {
        return getConnectionInstance(node).getOvsdbClient();
    }

    public Boolean getHasDeviceOwnership(final ConnectionInfo connectionInfo) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstance(connectionInfo);
        if (ovsdbConnectionInstance == null) {
            return Boolean.FALSE;
        }
        return ovsdbConnectionInstance.getHasDeviceOwnership();
    }

    public void reconcileConnection(final InstanceIdentifier<Node> iid, final OvsdbNodeAugmentation ovsdbNode) {
        retryConnection(iid, ovsdbNode,
                ConnectionReconciliationTriggers.ON_CONTROLLER_INITIATED_CONNECTION_FAILURE);

    }

    public void stopConnectionReconciliationIfActive(final InstanceIdentifier<Node> iid,
            final OvsdbNodeAugmentation ovsdbNode) {
        final ReconciliationTask task = new ConnectionReconciliationTask(
                reconciliationManager,
                this,
                iid,
                ovsdbNode);
        reconciliationManager.dequeue(task);
    }

    public void stopBridgeConfigReconciliationIfActive(final InstanceIdentifier<Node> iid) {
        final ReconciliationTask task =
                new BridgeConfigReconciliationTask(reconciliationManager, this, iid, null, instanceIdentifierCodec);
        reconciliationManager.dequeue(task);
        reconciliationManager.cancelTerminationPointReconciliation();
    }

    @VisibleForTesting
    void handleOwnershipChanged(final Entity entity, final EntityOwnershipStateChange change) {
        OvsdbConnectionInstance ovsdbConnectionInstance = getConnectionInstanceFromEntity(entity);
        LOG.debug("Ovsdb handleOwnershipChanged: {} event received for device {}", change,
            ovsdbConnectionInstance != null ? ovsdbConnectionInstance.getConnectionInfo()
                : "that's currently NOT registered by *this* southbound plugin instance");

        if (ovsdbConnectionInstance == null) {
            if (change.isOwner()) {
                LOG.warn("Ovsdb handleOwnershipChanged: *this* instance is elected as an owner of the device {} but it "
                        + "is NOT registered for ownership", entity);
            } else {
                // EntityOwnershipService sends notification to all the nodes, irrespective of whether
                // that instance registered for the device ownership or not. It is to make sure that
                // If all the controller instance that was connected to the device are down, so the
                // running instance can clear up the operational data store even though it was not
                // connected to the device.
                LOG.debug("Ovsdb handleOwnershipChanged: No connection instance found for {}",
                    entity);
            }

            // If entity has no owner, clean up the operational data store (it's possible because owner controller
            // might went down abruptly and didn't get a chance to clean up the operational data store.
            if (!change.hasOwner()) {
                LOG.info("Ovsdb {} has no owner, cleaning up the operational data store", entity);
                cleanEntityOperationalData(entity);
            }
            return;
        }
        //Connection detail need to be cached, irrespective of ownership result.
        putConnectionInstance(ovsdbConnectionInstance.getMDConnectionInfo(),ovsdbConnectionInstance);

        if (change.isOwner() == ovsdbConnectionInstance.getHasDeviceOwnership()) {
            LOG.info("Ovsdb handleOwnershipChanged: no change in ownership for {}. Ownership status is : {}",
                    ovsdbConnectionInstance.getConnectionInfo(), ovsdbConnectionInstance.getHasDeviceOwnership()
                            ? OwnershipStates.OWNER.getState()
                            : OwnershipStates.NONOWNER.getState());
            return;
        }

        ovsdbConnectionInstance.setHasDeviceOwnership(change.isOwner());
        // You were not an owner, but now you are
        if (change.isOwner()) {
            LOG.info("Ovsdb handleOwnershipChanged: *this* southbound plugin instance is an OWNER of the device {}",
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
            LOG.error("Ovsdb handleOwnershipChanged: *this* southbound plugin instance is no longer"
                    + " the owner of device {}.This should NOT happen.",
                    ovsdbConnectionInstance.getNodeId().getValue());
        }
    }

    private void cleanEntityOperationalData(final Entity entity) {

        //Do explicit cleanup rather than using OvsdbNodeRemoveCommand, because there
        // are chances that other controller instance went down abruptly and it does
        // not clear manager entry, which OvsdbNodeRemoveCommand look for before cleanup.

        @SuppressWarnings("unchecked")
        final InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>) entity.getIdentifier();

        txInvoker.invoke(transaction -> {
            Optional<Node> ovsdbNodeOpt = SouthboundUtil.readNode(transaction, nodeIid);
            if (ovsdbNodeOpt.isPresent()) {
                Node ovsdbNode = ovsdbNodeOpt.orElseThrow();
                OvsdbNodeAugmentation nodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
                if (nodeAugmentation != null) {
                    Map<ManagedNodeEntryKey, ManagedNodeEntry> entries = nodeAugmentation.getManagedNodeEntry();
                    if (entries != null) {
                        for (ManagedNodeEntry managedNode : entries.values()) {
                            transaction.delete(LogicalDatastoreType.OPERATIONAL,
                                (DataObjectIdentifier<?>) managedNode.getBridgeRef().getValue());
                        }
                    } else {
                        LOG.debug("{} had no managed nodes", ovsdbNode.getNodeId().getValue());
                    }
                }
                transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeIid);
            }
        });

    }

    private OpenVSwitch getOpenVswitchTableEntry(final OvsdbConnectionInstance connectionInstance) {
        final TypedDatabaseSchema dbSchema;
        try {
            dbSchema = connectionInstance.getSchema(OvsdbSchemaContants.DATABASE_NAME).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Ovsdb Not able to fetch schema for database {} from device {}",
                    OvsdbSchemaContants.DATABASE_NAME,connectionInstance.getConnectionInfo(),e);
            return null;
        }

        final GenericTableSchema openVSwitchSchema = dbSchema.getTableSchema(OpenVSwitch.class);
        final Select<GenericTableSchema> selectOperation = ops.select(openVSwitchSchema);
        selectOperation.setColumns(openVSwitchSchema.getColumnList());

        List<Operation> operations = new ArrayList<>();
        operations.add(selectOperation);
        operations.add(ops.comment("Fetching Open_VSwitch table rows"));
        final List<OperationResult> results;
        try {
            results = connectionInstance.transact(dbSchema, operations).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Ovsdb Not able to fetch OpenVswitch table row from device {}",
                connectionInstance.getConnectionInfo(), e);
            return null;
        }

        return results == null || results.isEmpty() ? null
                : dbSchema.getTypedRowWrapper(OpenVSwitch.class, results.get(0).getRows().get(0));
    }

    private Entity getEntityFromConnectionInstance(@NonNull final OvsdbConnectionInstance ovsdbConnectionInstance) {
        InstanceIdentifier<Node> iid = ovsdbConnectionInstance.getInstanceIdentifier();
        if (iid == null) {
            /* Switch initiated connection won't have iid, till it gets OpenVSwitch
             * table update but update callback is always registered after ownership
             * is granted. So we are explicitly fetch the row here to get the iid.
             */
            OpenVSwitch openvswitchRow = getOpenVswitchTableEntry(ovsdbConnectionInstance);
            iid = SouthboundMapper.getInstanceIdentifier(instanceIdentifierCodec, openvswitchRow);
            LOG.info("Ovsdb InstanceIdentifier {} generated for device "
                    + "connection {}",iid,ovsdbConnectionInstance.getConnectionInfo());
            ovsdbConnectionInstance.setInstanceIdentifier(iid);
        }
        Entity deviceEntity = new Entity(ENTITY_TYPE, iid);
        LOG.debug("Ovsdb Entity {} created for device connection {}",
                deviceEntity, ovsdbConnectionInstance.getConnectionInfo());
        return deviceEntity;
    }

    private OvsdbConnectionInstance getConnectionInstanceFromEntity(final Entity entity) {
        return entityConnectionMap.get(entity);
    }

    private void registerEntityForOwnership(final OvsdbConnectionInstance ovsdbConnectionInstance) {
        putConnectionInstance(ovsdbConnectionInstance.getMDConnectionInfo(), ovsdbConnectionInstance);

        Entity candidateEntity = getEntityFromConnectionInstance(ovsdbConnectionInstance);
        if (entityConnectionMap.containsKey(candidateEntity)) {
            LOG.error("Ovsdb Old connection still hanging for {}", candidateEntity);
            disconnected(ovsdbConnectionInstance.getOvsdbClient());
            //TODO do cleanup for old connection or stale check
        }
        nodeIdVsConnectionInstance.put((InstanceIdentifier<Node>) candidateEntity.getIdentifier(),
                ovsdbConnectionInstance);
        entityConnectionMap.put(candidateEntity, ovsdbConnectionInstance);
        ovsdbConnectionInstance.setConnectedEntity(candidateEntity);
        try {
            Registration registration = entityOwnershipService.registerCandidate(candidateEntity);
            ovsdbConnectionInstance.setDeviceOwnershipCandidateRegistration(registration);
            LOG.info("OVSDB entity {} is registered for ownership.", candidateEntity);

        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB entity {} was already registered for ownership", candidateEntity, e);
        }
        //If entity already has owner, it won't get notification from EntityOwnershipService
        Optional<EntityOwnershipState> ownershipStateOpt = entityOwnershipService.getOwnershipState(candidateEntity);
        if (ownershipStateOpt.isPresent()) {
            EntityOwnershipState ownershipState = ownershipStateOpt.orElseThrow();
            if (ownershipState == EntityOwnershipState.OWNED_BY_OTHER) {
                ovsdbConnectionInstance.setHasDeviceOwnership(false);
            } else if (ownershipState == EntityOwnershipState.IS_OWNER) {
                ovsdbConnectionInstance.setHasDeviceOwnership(true);
                ovsdbConnectionInstance.registerCallbacks(instanceIdentifierCodec);
            }
        }
    }

    private void unregisterEntityForOwnership(final OvsdbConnectionInstance ovsdbConnectionInstance) {
        ovsdbConnectionInstance.closeDeviceOwnershipCandidateRegistration();
        entityConnectionMap.remove(ovsdbConnectionInstance.getConnectedEntity(), ovsdbConnectionInstance);
    }

    private void retryConnection(final InstanceIdentifier<Node> iid, final OvsdbNodeAugmentation ovsdbNode,
                                 final ConnectionReconciliationTriggers trigger) {
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
            case ON_DISCONNECT: {
                FluentFuture<Boolean> readNodeFuture;
                try (ReadTransaction tx = db.newReadOnlyTransaction()) {
                    readNodeFuture = tx.exists(LogicalDatastoreType.CONFIGURATION, iid);
                }
                readNodeFuture.addCallback(new FutureCallback<Boolean>() {
                    @Override
                    public void onSuccess(final Boolean node) {
                        if (node) {
                            LOG.info("Disconnected/Failed connection {} was controller initiated, attempting "
                                    + "reconnection", ovsdbNode.getConnectionInfo());
                            reconciliationManager.enqueue(task);

                        } else {
                            LOG.debug("Connection {} was switch initiated, no reconciliation is required",
                                    iid.firstKeyOf(Node.class).getNodeId());
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOG.warn("Read Config/DS for Node failed! {}", iid, throwable);
                    }
                }, MoreExecutors.directExecutor());
                break;
            }
            default:
                break;
        }
    }

    private void reconcileBridgeConfigurations(final OvsdbConnectionInstance client) {
        reconciliationManager.enqueue(new BridgeConfigReconciliationTask(reconciliationManager, this,
            client.getInstanceIdentifier(), client, instanceIdentifierCodec));
    }

    private static final class OvsdbDeviceEntityOwnershipListener implements EntityOwnershipListener {
        private final OvsdbConnectionManager cm;
        private final Registration listenerRegistration;

        OvsdbDeviceEntityOwnershipListener(final OvsdbConnectionManager cm,
                final EntityOwnershipService entityOwnershipService) {
            this.cm = cm;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(final Entity entity, final EntityOwnershipStateChange change,
                final boolean inJeopardy) {
            cm.handleOwnershipChanged(entity, change);
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

    private enum OwnershipStates {
        OWNER("OWNER"),
        NONOWNER("NON-OWNER");

        private final String state;

        OwnershipStates(final String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }

        String getState() {
            return state;
        }
    }
}
