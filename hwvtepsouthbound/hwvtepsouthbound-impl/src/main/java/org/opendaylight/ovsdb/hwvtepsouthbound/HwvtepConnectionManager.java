/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.opendaylight.ovsdb.hwvtepsouthbound.events.ClientConnected;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration.HwvtepReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependencyQueue;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.HwvtepGlobalRemoveCommand;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepConnectionManager implements OvsdbConnectionListener, AutoCloseable {
    private final Map<ConnectionInfo, HwvtepConnectionInstance> clients = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepConnectionManager.class);
    private static final String ENTITY_TYPE = "hwvtep";
    private static final int DB_FETCH_TIMEOUT = 1000;
    private static final int TRANSACTION_HISTORY_CAPACITY = 10000;
    private static final int TRANSACTION_HISTORY_WATERMARK = 7500;

    private final DataBroker db;
    private final TransactionInvoker txInvoker;
    private final Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers = new ConcurrentHashMap<>();
    private final Map<Entity, HwvtepConnectionInstance> entityConnectionMap = new ConcurrentHashMap<>();
    private final EntityOwnershipService entityOwnershipService;
    private final HwvtepDeviceEntityOwnershipListener hwvtepDeviceEntityOwnershipListener;
    private final ReconciliationManager reconciliationManager;
    private final Map<InstanceIdentifier<Node>, HwvtepConnectionInstance> nodeIidVsConnectionInstance =
            new ConcurrentHashMap<>();
    private final HwvtepOperGlobalListener hwvtepOperGlobalListener;
    private final Map<InstanceIdentifier<Node>, TransactionHistory> controllerTxHistory = new ConcurrentHashMap<>();
    private final Map<InstanceIdentifier<Node>, TransactionHistory> deviceUpdateHistory = new ConcurrentHashMap<>();

    public HwvtepConnectionManager(DataBroker db, TransactionInvoker txInvoker,
                    EntityOwnershipService entityOwnershipService) {
        this.db = db;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        this.hwvtepDeviceEntityOwnershipListener = new HwvtepDeviceEntityOwnershipListener(this,entityOwnershipService);
        this.reconciliationManager = new ReconciliationManager(db);
        this.hwvtepOperGlobalListener = new HwvtepOperGlobalListener(db, this);
    }

    @Override
    public void close() throws Exception {
        if (hwvtepDeviceEntityOwnershipListener != null) {
            hwvtepDeviceEntityOwnershipListener.close();
        }

        for (HwvtepConnectionInstance client: clients.values()) {
            client.disconnect();
        }
        DependencyQueue.close();
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
            if (databases.contains(HwvtepSchemaConstants.HARDWARE_VTEP)) {
                HwvtepConnectionInstance hwClient = connectedButCallBacksNotRegistered(externalClient);
                registerEntityForOwnership(hwClient);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to fetch Database list from device {}. Disconnecting from the device.",
                    externalClient.getConnectionInfo().getRemoteAddress(), e);
            externalClient.disconnect();
        }
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("Library disconnected {} from {}:{} to {}:{}. Cleaning up the operational data store",
                client.getConnectionInfo().getType(),
                client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort(),
                client.getConnectionInfo().getLocalAddress(),
                client.getConnectionInfo().getLocalPort());
        ConnectionInfo key = HwvtepSouthboundMapper.createConnectionInfo(client);
        HwvtepConnectionInstance hwvtepConnectionInstance = getConnectionInstance(key);
        if (hwvtepConnectionInstance != null) {
            deviceUpdateHistory.get(hwvtepConnectionInstance.getInstanceIdentifier()).addToHistory(
                    TransactionType.DELETE, new ClientConnected(client.getConnectionInfo().getRemotePort()));

            // Unregister Entity ownership as soon as possible ,so this instance should
            // not be used as a candidate in Entity election (given that this instance is
            // about to disconnect as well), if current owner get disconnected from
            // HWVTEP device.
            if (hwvtepConnectionInstance.getHasDeviceOwnership()) {
                unregisterEntityForOwnership(hwvtepConnectionInstance);
                txInvoker.invoke(new HwvtepGlobalRemoveCommand(hwvtepConnectionInstance, null, null));
            } else {
                unregisterEntityForOwnership(hwvtepConnectionInstance);
                //Do not delete if client disconnected from follower HwvtepGlobalRemoveCommand
            }

            removeConnectionInstance(key);

            //Controller initiated connection can be terminated from switch side.
            //So cleanup the instance identifier cache.
            removeInstanceIdentifier(key);
            removeConnectionInstance(hwvtepConnectionInstance.getInstanceIdentifier());
            retryConnection(hwvtepConnectionInstance.getInstanceIdentifier(),
                    hwvtepConnectionInstance.getHwvtepGlobalAugmentation(),
                    ConnectionReconciliationTriggers.ON_DISCONNECT);
        } else {
            LOG.warn("HWVTEP disconnected event did not find connection instance for {}", key);
        }
        LOG.trace("HwvtepConnectionManager exit disconnected client: {}", client);
    }

    public OvsdbClient connect(InstanceIdentifier<Node> iid,
                               HwvtepGlobalAugmentation hwvtepGlobal) throws UnknownHostException, ConnectException {
        LOG.info("Connecting to {}", HwvtepSouthboundUtil.connectionInfoToString(hwvtepGlobal.getConnectionInfo()));
        InetAddress ip = HwvtepSouthboundMapper.createInetAddress(hwvtepGlobal.getConnectionInfo().getRemoteIp());
        OvsdbClient client = OvsdbConnectionService.getService()
                        .connect(ip, hwvtepGlobal.getConnectionInfo().getRemotePort().getValue());
        if (client != null) {
            putInstanceIdentifier(hwvtepGlobal.getConnectionInfo(), iid.firstIdentifierOf(Node.class));
            HwvtepConnectionInstance hwvtepConnectionInstance = connectedButCallBacksNotRegistered(client);
            hwvtepConnectionInstance.setHwvtepGlobalAugmentation(hwvtepGlobal);
            hwvtepConnectionInstance.setInstanceIdentifier(iid.firstIdentifierOf(Node.class));

            // Register Cluster Ownership for ConnectionInfo
            registerEntityForOwnership(hwvtepConnectionInstance);
        } else {
            LOG.warn("Failed to connect to OVSDB node: {}", hwvtepGlobal.getConnectionInfo());
        }
        return client;
    }

    public void disconnect(HwvtepGlobalAugmentation ovsdbNode) throws UnknownHostException {
        LOG.info("Diconnecting from {}", HwvtepSouthboundUtil.connectionInfoToString(ovsdbNode.getConnectionInfo()));
        HwvtepConnectionInstance client = getConnectionInstance(ovsdbNode.getConnectionInfo());
        if (client != null) {
            client.disconnect();
            // Unregister Cluster Ownership for ConnectionInfo
            unregisterEntityForOwnership(client);
            removeInstanceIdentifier(ovsdbNode.getConnectionInfo());
        }
    }

    public HwvtepConnectionInstance connectedButCallBacksNotRegistered(final OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        ConnectionInfo key = HwvtepSouthboundMapper.createConnectionInfo(externalClient);
        HwvtepConnectionInstance hwvtepConnectionInstance = getConnectionInstance(key);

        // Check if existing hwvtepConnectionInstance for the OvsdbClient present.
        // In such cases, we will see if the hwvtepConnectionInstance has same externalClient.
        if (hwvtepConnectionInstance != null) {
            if (hwvtepConnectionInstance.hasOvsdbClient(externalClient)) {
                LOG.warn("HWVTEP Connection Instance {} already exists for client {}", key, externalClient);
                return hwvtepConnectionInstance;
            }
            LOG.warn("HWVTEP Connection Instance {} being replaced with client {}", key, externalClient);
            hwvtepConnectionInstance.disconnect();

            // Unregister Cluster Ownership for ConnectionInfo
            // Because the hwvtepConnectionInstance is about to be completely replaced!
            unregisterEntityForOwnership(hwvtepConnectionInstance);

            removeConnectionInstance(key);
        }

        hwvtepConnectionInstance = new HwvtepConnectionInstance(this, key, externalClient, getInstanceIdentifier(key),
                txInvoker, db);
        hwvtepConnectionInstance.createTransactInvokers();
        return hwvtepConnectionInstance;
    }

    private void putConnectionInstance(ConnectionInfo key,HwvtepConnectionInstance instance) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
        LOG.info("Clients after put: {}", clients);
    }

    void putConnectionInstance(final InstanceIdentifier<Node> nodeIid,
            final HwvtepConnectionInstance connectionInstance) {
        nodeIidVsConnectionInstance.put(nodeIid, connectionInstance);
    }

    public HwvtepConnectionInstance getConnectionInstanceFromNodeIid(final InstanceIdentifier<Node> nodeIid) {
        HwvtepConnectionInstance hwvtepConnectionInstance = nodeIidVsConnectionInstance.get(nodeIid);
        if (hwvtepConnectionInstance != null) {
            return hwvtepConnectionInstance;
        }
        InstanceIdentifier<Node> globalNodeIid = HwvtepSouthboundUtil.getGlobalNodeIid(nodeIid);
        if (globalNodeIid != null) {
            return nodeIidVsConnectionInstance.get(globalNodeIid);
        }
        return null;
    }

    public HwvtepConnectionInstance getConnectionInstance(final ConnectionInfo key) {
        if (key == null) {
            return null;
        }
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
    }

    public HwvtepConnectionInstance getConnectionInstance(Node node) {
        Preconditions.checkNotNull(node);
        HwvtepGlobalAugmentation hwvtepGlobal = node.getAugmentation(HwvtepGlobalAugmentation.class);
        PhysicalSwitchAugmentation switchNode = node.getAugmentation(PhysicalSwitchAugmentation.class);
        if (hwvtepGlobal != null) {
            if (hwvtepGlobal.getConnectionInfo() != null) {
                return getConnectionInstance(hwvtepGlobal.getConnectionInfo());
            } else {
                //TODO: Case of user configured connection for now
                //TODO: We could get it from Managers also.
                return null;
            }
        }
        else if (switchNode != null) {
            return getConnectionInstance(switchNode);
        } else {
            LOG.warn("This is not a node that gives any hint how to find its OVSDB Manager: {}",node);
            return null;
        }
    }

    public HwvtepConnectionInstance getConnectionInstance(HwvtepPhysicalSwitchAttributes node) {
        Optional<HwvtepGlobalAugmentation> optional = HwvtepSouthboundUtil.getManagingNode(db, node);
        if (optional.isPresent()) {
            return getConnectionInstance(optional.get().getConnectionInfo());
        } else {
            return null;
        }
    }

    public void stopConfigurationReconciliation(final InstanceIdentifier<Node> nodeIid) {
        final ReconciliationTask task = new HwvtepReconciliationTask(
                reconciliationManager, HwvtepConnectionManager.this, nodeIid, null, null, db);

        reconciliationManager.dequeue(task);
    }

    public void reconcileConfigurations(final HwvtepConnectionInstance client, Node psNode) {
        final InstanceIdentifier<Node> nodeIid = client.getInstanceIdentifier();
        final ReconciliationTask task = new HwvtepReconciliationTask(
                reconciliationManager, HwvtepConnectionManager.this, nodeIid, psNode, client, db);

        reconciliationManager.enqueue(task);
    }

    private void removeConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.remove(connectionInfo);
        LOG.info("Clients after remove: {}", clients);
    }

    private void removeConnectionInstance(final InstanceIdentifier<Node> nodeIid) {
        if (nodeIid != null) {
            nodeIidVsConnectionInstance.remove(nodeIid);
        }
    }

    private void putInstanceIdentifier(ConnectionInfo key,InstanceIdentifier<Node> iid) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.put(connectionInfo, iid);
    }

    public InstanceIdentifier<Node> getInstanceIdentifier(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        return instanceIdentifiers.get(connectionInfo);
    }

    private void removeInstanceIdentifier(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.remove(connectionInfo);
    }

    public OvsdbClient getClient(ConnectionInfo connectionInfo) {
        return getConnectionInstance(connectionInfo).getOvsdbClient();
    }

    private void registerEntityForOwnership(HwvtepConnectionInstance hwvtepConnectionInstance) {

        Entity candidateEntity = getEntityFromConnectionInstance(hwvtepConnectionInstance);
        if (entityConnectionMap.get(candidateEntity) != null) {
            disconnected(entityConnectionMap.get(candidateEntity).getOvsdbClient());
            putConnectionInstance(hwvtepConnectionInstance.getInstanceIdentifier(), hwvtepConnectionInstance);
        }
        entityConnectionMap.put(candidateEntity, hwvtepConnectionInstance);
        hwvtepConnectionInstance.setConnectedEntity(candidateEntity);

        try {
            EntityOwnershipCandidateRegistration registration =
                    entityOwnershipService.registerCandidate(candidateEntity);
            hwvtepConnectionInstance.setDeviceOwnershipCandidateRegistration(registration);
            LOG.info("HWVTEP entity {} is registered for ownership.", candidateEntity);

            //If entity already has owner, it won't get notification from EntityOwnershipService
            //so cache the connection instances.
            handleOwnershipState(candidateEntity, hwvtepConnectionInstance);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB entity {} was already registered for ownership", candidateEntity, e);
        }

    }

    private void handleOwnershipState(Entity candidateEntity, HwvtepConnectionInstance hwvtepConnectionInstance) {
        //If entity already has owner, it won't get notification from EntityOwnershipService
        //so cache the connection instances.
        Optional<EntityOwnershipState> ownershipStateOpt =
                entityOwnershipService.getOwnershipState(candidateEntity);
        if (ownershipStateOpt.isPresent()) {
            EntityOwnershipState ownershipState = ownershipStateOpt.get();
            putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(), hwvtepConnectionInstance);
            if (ownershipState.hasOwner()) {
                hwvtepConnectionInstance.setHasDeviceOwnership(ownershipState.isOwner());
                if (!ownershipState.isOwner()) {
                    LOG.info("HWVTEP entity {} is already owned by other southbound plugin "
                                    + "instance, so *this* instance is NOT an OWNER of the device",
                            hwvtepConnectionInstance.getConnectionInfo());
                } else {
                    afterTakingOwnership(hwvtepConnectionInstance);
                }
            }
        }
    }

    private void afterTakingOwnership(HwvtepConnectionInstance hwvtepConnectionInstance) {
        txInvoker.invoke(new HwvtepGlobalRemoveCommand(hwvtepConnectionInstance, null, null));
        putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(), hwvtepConnectionInstance);
        hwvtepConnectionInstance.setHasDeviceOwnership(true);
        hwvtepConnectionInstance.registerCallbacks();
    }

    private Global getHwvtepGlobalTableEntry(HwvtepConnectionInstance connectionInstance) {
        DatabaseSchema dbSchema = null;
        Global globalRow = null;

        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    HwvtepSchemaConstants.HARDWARE_VTEP,connectionInstance.getConnectionInfo(),e);
        }

        if (dbSchema != null) {
            GenericTableSchema hwvtepSchema = TyperUtils.getTableSchema(dbSchema, Global.class);

            List<String> hwvtepTableColumn = new ArrayList<>();
            hwvtepTableColumn.addAll(hwvtepSchema.getColumns());
            Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
            selectOperation.setColumns(hwvtepTableColumn);

            ArrayList<Operation> operations = new ArrayList<>();
            operations.add(selectOperation);
            operations.add(op.comment("Fetching hardware_vtep table rows"));

            try {
                List<OperationResult> results = connectionInstance.transact(dbSchema, operations).get();
                if (results != null) {
                    OperationResult selectResult = results.get(0);
                    globalRow = TyperUtils.getTypedRowWrapper(
                            dbSchema,Global.class,selectResult.getRows().get(0));
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Not able to fetch hardware_vtep table row from device {}",
                        connectionInstance.getConnectionInfo(),e);
            }
        }
        LOG.trace("Fetched global {} from hardware_vtep schema",globalRow);
        return globalRow;
    }

    private Entity getEntityFromConnectionInstance(@Nonnull HwvtepConnectionInstance hwvtepConnectionInstance) {
        InstanceIdentifier<Node> iid = hwvtepConnectionInstance.getInstanceIdentifier();
        if (iid == null) {
            //TODO: Is Global the right one?
            Global hwvtepGlobalRow = getHwvtepGlobalTableEntry(hwvtepConnectionInstance);
            iid = HwvtepSouthboundMapper.getInstanceIdentifier(hwvtepGlobalRow);
            /* Let's set the iid now */
            hwvtepConnectionInstance.setInstanceIdentifier(iid);
            LOG.info("InstanceIdentifier {} generated for device "
                    + "connection {}",iid, hwvtepConnectionInstance.getConnectionInfo());
            controllerTxHistory.putIfAbsent(iid,
                    new TransactionHistory(TRANSACTION_HISTORY_CAPACITY, TRANSACTION_HISTORY_WATERMARK));
            deviceUpdateHistory.putIfAbsent(iid,
                    new TransactionHistory(TRANSACTION_HISTORY_CAPACITY, TRANSACTION_HISTORY_WATERMARK));
            TransactionHistory controllerLog = controllerTxHistory.get(iid);
            TransactionHistory deviceLog = deviceUpdateHistory.get(iid);
            int port = hwvtepConnectionInstance.getOvsdbClient().getConnectionInfo().getRemotePort();
            deviceLog.addToHistory(TransactionType.ADD, new ClientConnected(port));
            hwvtepConnectionInstance.setControllerTxHistory(controllerLog);
            hwvtepConnectionInstance.setDeviceUpdateHistory(deviceLog);
        }
        YangInstanceIdentifier entityId =
                HwvtepSouthboundUtil.getInstanceIdentifierCodec().getYangInstanceIdentifier(iid);
        Entity deviceEntity = new Entity(ENTITY_TYPE, entityId);
        LOG.debug("Entity {} created for device connection {}",
                deviceEntity, hwvtepConnectionInstance.getConnectionInfo());
        return deviceEntity;
    }

    private void unregisterEntityForOwnership(HwvtepConnectionInstance hwvtepConnectionInstance) {
        hwvtepConnectionInstance.closeDeviceOwnershipCandidateRegistration();
        entityConnectionMap.remove(hwvtepConnectionInstance.getConnectedEntity());
    }

    public void reconcileConnection(InstanceIdentifier<Node> iid, HwvtepGlobalAugmentation hwvtepNode) {
        this.retryConnection(iid, hwvtepNode,
                ConnectionReconciliationTriggers.ON_CONTROLLER_INITIATED_CONNECTION_FAILURE);
    }

    public void stopConnectionReconciliationIfActive(InstanceIdentifier<?> iid, HwvtepGlobalAugmentation hwvtepNode) {
        final ReconciliationTask task = new ConnectionReconciliationTask(
                reconciliationManager,
                this,
                iid,
                hwvtepNode);
        reconciliationManager.dequeue(task);
    }

    private void retryConnection(final InstanceIdentifier<Node> iid, final HwvtepGlobalAugmentation hwvtepNode,
                                 ConnectionReconciliationTriggers trigger) {
        final ReconciliationTask task = new ConnectionReconciliationTask(
                reconciliationManager,
                this,
                iid,
                hwvtepNode);

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

                Futures.addCallback(readNodeFuture, new FutureCallback<Optional<Node>>() {
                    @Override
                    public void onSuccess(@Nullable Optional<Node> node) {
                        if (node.isPresent()) {
                            HwvtepGlobalAugmentation augmentation = node.get()
                                    .getAugmentation(HwvtepGlobalAugmentation.class);
                            if (augmentation == null || augmentation.getConnectionInfo() == null) {
                                return;
                            }
                            LOG.info(
                                "Disconnected/Failed connection {} was controller initiated, attempting reconnection",
                                hwvtepNode.getConnectionInfo());
                            reconciliationManager.enqueue(task);

                        } else {
                            LOG.debug("Connection {} was switch initiated, no reconciliation is required",
                                iid.firstKeyOf(Node.class).getNodeId());
                        }
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        LOG.warn("Read Config/DS for Node failed! {}", iid, ex);
                    }
                }, MoreExecutors.directExecutor());
                break;
            }
            default:
                break;
        }
    }

    public void handleOwnershipChanged(EntityOwnershipChange ownershipChange) {
        HwvtepConnectionInstance hwvtepConnectionInstance =
                getConnectionInstanceFromEntity(ownershipChange.getEntity());
        LOG.info("handleOwnershipChanged: {} event received for device {}",
                ownershipChange, hwvtepConnectionInstance != null ? hwvtepConnectionInstance.getConnectionInfo()
                        : "THAT'S NOT REGISTERED BY THIS SOUTHBOUND PLUGIN INSTANCE");

        if (hwvtepConnectionInstance == null) {
            if (ownershipChange.isOwner()) {
                LOG.warn("handleOwnershipChanged: found no connection instance for {}", ownershipChange.getEntity());
            } else {
                // EntityOwnershipService sends notification to all the nodes, irrespective of whether
                // that instance registered for the device ownership or not. It is to make sure that
                // If all the controller instance that was connected to the device are down, so the
                // running instance can clear up the operational data store even though it was not
                // connected to the device.
                LOG.debug("handleOwnershipChanged: found no connection instance for {}", ownershipChange.getEntity());
            }

            // If entity has no owner, clean up the operational data store (it's possible because owner controller
            // might went down abruptly and didn't get a chance to clean up the operational data store.
            if (!ownershipChange.hasOwner()) {
                LOG.debug("{} has no owner, cleaning up the operational data store", ownershipChange.getEntity());
                // If first cleanEntityOperationalData() was called, this call will be no-op.
                cleanEntityOperationalData(ownershipChange.getEntity());
            }
            return;
        }
        //Connection detail need to be cached, irrespective of ownership result.
        putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(), hwvtepConnectionInstance);

        if (ownershipChange.isOwner() == hwvtepConnectionInstance.getHasDeviceOwnership()) {
            LOG.debug("handleOwnershipChanged: no change in ownership for {}. Ownership status is : {}",
                    hwvtepConnectionInstance.getConnectionInfo(), hwvtepConnectionInstance.getHasDeviceOwnership());
            return;
        }

        hwvtepConnectionInstance.setHasDeviceOwnership(ownershipChange.isOwner());
        // You were not an owner, but now you are
        if (ownershipChange.isOwner()) {
            LOG.info("handleOwnershipChanged: *this* southbound plugin instance is owner of device {}",
                    hwvtepConnectionInstance.getConnectionInfo());

            //*this* instance of southbound plugin is owner of the device,
            //so register for monitor callbacks
            afterTakingOwnership(hwvtepConnectionInstance);

        } else {
            //You were owner of the device, but now you are not. With the current ownership
            //grant mechanism, this scenario should not occur. Because this scenario will occur
            //when this controller went down or switch flap the connection, but in both the case
            //it will go through the re-registration process. We need to implement this condition
            //when clustering service implement a ownership grant strategy which can revoke the
            //device ownership for load balancing the devices across the instances.
            //Once this condition occur, we should unregister the callback.
            LOG.error("handleOwnershipChanged: *this* southbound plugin instance is no longer the owner of device {}",
                    hwvtepConnectionInstance.getNodeId().getValue());
        }
    }

    private void cleanEntityOperationalData(Entity entity) {
        @SuppressWarnings("unchecked") final InstanceIdentifier<Node> nodeIid =
                (InstanceIdentifier<Node>) HwvtepSouthboundUtil
                        .getInstanceIdentifierCodec().bindingDeserializer(entity.getId());
        txInvoker.invoke(new HwvtepGlobalRemoveCommand(nodeIid));
    }

    private HwvtepConnectionInstance getConnectionInstanceFromEntity(Entity entity) {
        return entityConnectionMap.get(entity);
    }

    public Map<InstanceIdentifier<Node>, TransactionHistory> getControllerTxHistory() {
        return controllerTxHistory;
    }

    public Map<InstanceIdentifier<Node>, TransactionHistory> getDeviceUpdateHistory() {
        return deviceUpdateHistory;
    }

    private class HwvtepDeviceEntityOwnershipListener implements EntityOwnershipListener {
        private final HwvtepConnectionManager hcm;
        private final EntityOwnershipListenerRegistration listenerRegistration;

        HwvtepDeviceEntityOwnershipListener(HwvtepConnectionManager hcm,
                EntityOwnershipService entityOwnershipService) {
            this.hcm = hcm;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            hcm.handleOwnershipChanged(ownershipChange);
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
