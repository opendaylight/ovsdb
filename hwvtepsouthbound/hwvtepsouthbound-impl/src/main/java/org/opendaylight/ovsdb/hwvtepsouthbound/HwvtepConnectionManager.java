/*
 * Copyright (c) 2015, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import org.opendaylight.ovsdb.hwvtepsouthbound.events.ClientConnected;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration.HwvtepReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependencyQueue;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.HwvtepGlobalRemoveCommand;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private final OvsdbConnection ovsdbConnectionService;
    private final Map<OvsdbClient, OvsdbClient> alreadyProcessedClients = new ConcurrentHashMap<>();

    public HwvtepConnectionManager(final DataBroker db, final TransactionInvoker txInvoker,
                    final EntityOwnershipService entityOwnershipService, final OvsdbConnection ovsdbConnectionService) {
        this.db = db;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        hwvtepDeviceEntityOwnershipListener = new HwvtepDeviceEntityOwnershipListener(this,entityOwnershipService);
        reconciliationManager = new ReconciliationManager(db);
        hwvtepOperGlobalListener = new HwvtepOperGlobalListener(db, this);
        this.ovsdbConnectionService = ovsdbConnectionService;
    }

    @Override
    public void close() {
        if (hwvtepDeviceEntityOwnershipListener != null) {
            hwvtepDeviceEntityOwnershipListener.close();
        }
        if (hwvtepOperGlobalListener != null) {
            hwvtepOperGlobalListener.close();
        }

        for (HwvtepConnectionInstance client : clients.values()) {
            client.disconnect();
        }
        DependencyQueue.close();
    }

    @Override
    public void connected(final OvsdbClient externalClient) {
        if (alreadyProcessedClients.containsKey(externalClient)) {
            LOG.info("Hwvtep Library already connected {} from {}:{} to {}:{} to this, hence skipping the processing",
                    externalClient.getConnectionInfo().getType(),
                    externalClient.getConnectionInfo().getRemoteAddress(),
                    externalClient.getConnectionInfo().getRemotePort(),
                    externalClient.getConnectionInfo().getLocalAddress(),
                    externalClient.getConnectionInfo().getLocalPort());
            return;
        }
        alreadyProcessedClients.put(externalClient, externalClient);
        HwvtepConnectionInstance hwClient = null;
        try {
            List<String> databases = externalClient.getDatabases().get(DB_FETCH_TIMEOUT, TimeUnit.MILLISECONDS);
            if (databases != null && !databases.isEmpty() && databases.contains(HwvtepSchemaConstants.HARDWARE_VTEP)) {
                LOG.info("Hwvtep Library connected {} from {}:{} to {}:{}",
                        externalClient.getConnectionInfo().getType(),
                        externalClient.getConnectionInfo().getRemoteAddress(),
                        externalClient.getConnectionInfo().getRemotePort(),
                        externalClient.getConnectionInfo().getLocalAddress(),
                        externalClient.getConnectionInfo().getLocalPort());
                hwClient = connectedButCallBacksNotRegistered(externalClient);
                registerEntityForOwnership(hwClient);
                HwvtepOperGlobalListener.runAfterTimeoutIfNodeNotCreated(hwClient.getInstanceIdentifier(), () -> {
                    externalClient.disconnect();
                    disconnected(externalClient);
                });
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to fetch Database list from device {}. Disconnecting from the device.",
                    externalClient.getConnectionInfo().getRemoteAddress(), e);
            externalClient.disconnect();
        }
    }

    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void disconnected(final OvsdbClient client) {
        alreadyProcessedClients.remove(client);
        HwvtepConnectionInstance hwvtepConnectionInstance = null;
        try {
            LOG.info("Library disconnected {} from {}:{} to {}:{}. Cleaning up the operational data store",
                    client.getConnectionInfo().getType(),
                    client.getConnectionInfo().getRemoteAddress(),
                    client.getConnectionInfo().getRemotePort(),
                    client.getConnectionInfo().getLocalAddress(),
                    client.getConnectionInfo().getLocalPort());
            ConnectionInfo key = HwvtepSouthboundMapper.createConnectionInfo(client);
            hwvtepConnectionInstance = getConnectionInstance(key);
            if (hwvtepConnectionInstance != null) {
                if (hwvtepConnectionInstance.getInstanceIdentifier() != null) {
                    int port = hwvtepConnectionInstance.getOvsdbClient().getConnectionInfo().getRemotePort();
                    deviceUpdateHistory.get(hwvtepConnectionInstance.getInstanceIdentifier()).addToHistory(
                            TransactionType.DELETE, new ClientConnected(client.getConnectionInfo().getRemotePort()));
                    LOG.info("CONTROLLER - {} {}", TransactionType.DELETE, new ClientConnected(port));
                }


                // Unregister Entity ownership as soon as possible ,so this instance should
                // not be used as a candidate in Entity election (given that this instance is
                // about to disconnect as well), if current owner get disconnected from
                // HWVTEP device.
                if (hwvtepConnectionInstance.getHasDeviceOwnership()) {
                    unregisterEntityForOwnership(hwvtepConnectionInstance);
                    LOG.info("Client disconnected from the Leader. Delete the Hvtep Node {} ",
                        hwvtepConnectionInstance.getInstanceIdentifier());
                    txInvoker.invoke(new HwvtepGlobalRemoveCommand(hwvtepConnectionInstance, null, null));
                } else {
                    unregisterEntityForOwnership(hwvtepConnectionInstance);
                    LOG.info("Client disconnected from the Follower. Not deleteing the Hvtep Node {} ",
                        hwvtepConnectionInstance.getInstanceIdentifier());

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
        } catch (Exception e) {
            LOG.error("Failed to execute disconnected ",e);
        }
    }

    public OvsdbClient connect(final InstanceIdentifier<Node> iid, final HwvtepGlobalAugmentation hwvtepGlobal)
            throws UnknownHostException, ConnectException {
        LOG.info("Connecting to {}", HwvtepSouthboundUtil.connectionInfoToString(hwvtepGlobal.getConnectionInfo()));
        InetAddress ip = HwvtepSouthboundMapper.createInetAddress(hwvtepGlobal.getConnectionInfo().getRemoteIp());
        OvsdbClient client = ovsdbConnectionService
                        .connect(ip, hwvtepGlobal.getConnectionInfo().getRemotePort().getValue().toJava());
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

    public void disconnect(final HwvtepGlobalAugmentation ovsdbNode) throws UnknownHostException {
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
                LOG.info("HWVTEP Connection Instance {} already exists for client {}", key, externalClient);
                return hwvtepConnectionInstance;
            }
            LOG.info("HWVTEP Connection Instance {} being replaced with client {}", key, externalClient);
            hwvtepConnectionInstance.disconnect();

            // Unregister Cluster Ownership for ConnectionInfo
            // Because the hwvtepConnectionInstance is about to be completely replaced!
            unregisterEntityForOwnership(hwvtepConnectionInstance);

            removeConnectionInstance(key);
        }

        hwvtepConnectionInstance = new HwvtepConnectionInstance(this, key,
                externalClient, getInstanceIdentifier(key), txInvoker, db);
        hwvtepConnectionInstance.createTransactInvokers();
        return hwvtepConnectionInstance;
    }

    private void putConnectionInstance(final ConnectionInfo key,final HwvtepConnectionInstance instance) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
        LOG.info("Clients after put: {}", clients);
    }

    void putConnectionInstance(final InstanceIdentifier<Node> nodeIid,
                                       final HwvtepConnectionInstance connectionInstance) {
        nodeIidVsConnectionInstance.put(nodeIid, connectionInstance);
    }

    public HwvtepConnectionInstance getConnectionInstance(final ConnectionInfo key) {
        if (key == null) {
            return null;
        }
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
    }

    public HwvtepConnectionInstance getConnectionInstance(final Node node) {
        requireNonNull(node);
        HwvtepGlobalAugmentation hwvtepGlobal = node.augmentation(HwvtepGlobalAugmentation.class);
        PhysicalSwitchAugmentation switchNode = node.augmentation(PhysicalSwitchAugmentation.class);
        if (hwvtepGlobal != null) {
            if (hwvtepGlobal.getConnectionInfo() != null) {
                LOG.debug("Get the ConnectionInfo from HwvtepGlobal {}", hwvtepGlobal.getConnectionInfo());
                return getConnectionInstance(hwvtepGlobal.getConnectionInfo());
            } else {
                //TODO: Case of user configured connection for now
                //TODO: We could get it from Managers also.
                return null;
            }
        }
        else if (switchNode != null) {
            LOG.debug("Get the ConnectionInfo from PhysicalSwitch");
            return getConnectionInstance(switchNode);
        } else {
            LOG.warn("This is not a node that gives any hint how to find its OVSDB Manager: {}",node);
            return null;
        }
    }

    public HwvtepConnectionInstance getConnectionInstance(final HwvtepPhysicalSwitchAttributes node) {
        Optional<HwvtepGlobalAugmentation> optional = HwvtepSouthboundUtil.getManagingNode(db, node);
        if (optional.isPresent()) {
            return getConnectionInstance(optional.orElseThrow().getConnectionInfo());
        } else {
            return null;
        }
    }

    public HwvtepConnectionInstance getConnectionInstanceFromNodeIid(final InstanceIdentifier<Node> nodeIid) {
        HwvtepConnectionInstance hwvtepConnectionInstance = nodeIidVsConnectionInstance.get(nodeIid);
        if (hwvtepConnectionInstance != null) {
            return hwvtepConnectionInstance;
        }
        InstanceIdentifier<Node> globalNodeIid = HwvtepSouthboundUtil.getGlobalNodeIid(nodeIid);
        if (globalNodeIid != null) {
            LOG.debug("Get the ConnectionInfo from HwvtepGlobal : {}", globalNodeIid);
            return nodeIidVsConnectionInstance.get(globalNodeIid);
        }
        return null;
    }

    public void stopConfigurationReconciliation(final InstanceIdentifier<Node> nodeIid) {
        final ReconciliationTask task = new HwvtepReconciliationTask(
                reconciliationManager, HwvtepConnectionManager.this, nodeIid, null, null, db);

        reconciliationManager.dequeue(task);
    }

    public void reconcileConfigurations(final HwvtepConnectionInstance client, final Node psNode) {
        final InstanceIdentifier<Node> nodeIid = client.getInstanceIdentifier();
        final ReconciliationTask task = new HwvtepReconciliationTask(
                reconciliationManager, HwvtepConnectionManager.this, nodeIid, psNode, client, db);

        reconciliationManager.enqueue(task);
    }

    private void removeConnectionInstance(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.remove(connectionInfo);
        LOG.info("Clients after remove: {}", clients);
    }

    private void removeConnectionInstance(final InstanceIdentifier<Node> nodeIid) {
        if (nodeIid != null) {
            nodeIidVsConnectionInstance.remove(nodeIid);
        }
    }

    private void putInstanceIdentifier(final ConnectionInfo key,final InstanceIdentifier<Node> iid) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.put(connectionInfo, iid);
    }

    public InstanceIdentifier<Node> getInstanceIdentifier(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        return instanceIdentifiers.get(connectionInfo);
    }

    private void removeInstanceIdentifier(final ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        instanceIdentifiers.remove(connectionInfo);
    }

    public OvsdbClient getClient(final ConnectionInfo connectionInfo) {
        return getConnectionInstance(connectionInfo).getOvsdbClient();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void registerCallbacks(final HwvtepConnectionInstance hwvtepConnectionInstance) {
        LOG.info("HWVTEP entity {} is owned by this controller registering callbacks",
                hwvtepConnectionInstance.getConnectionInfo());
        try {
            hwvtepOperGlobalListener.runAfterNodeDeleted(
                hwvtepConnectionInstance.getInstanceIdentifier(), () -> {
                    cleanupOperationalNode(hwvtepConnectionInstance.getInstanceIdentifier());
                    hwvtepConnectionInstance.registerCallbacks();
                    return null;
                });
        } catch (Exception e) {
            LOG.error("Failed to register callbacks for HWVTEP entity {} ",
                    hwvtepConnectionInstance.getConnectionInfo(), e);
        }
    }


    private void registerEntityForOwnership(final HwvtepConnectionInstance hwvtepConnectionInstance) {

        Entity candidateEntity = getEntityFromConnectionInstance(hwvtepConnectionInstance);
        if (entityConnectionMap.get(candidateEntity) != null) {
            InstanceIdentifier<Node> iid = hwvtepConnectionInstance.getInstanceIdentifier();
            LOG.info("Calling disconnect before processing new connection for {}", candidateEntity);
            disconnected(entityConnectionMap.get(candidateEntity).getOvsdbClient());
            hwvtepConnectionInstance.setInstanceIdentifier(iid);
            putConnectionInstance(hwvtepConnectionInstance.getInstanceIdentifier(), hwvtepConnectionInstance);
        }
        entityConnectionMap.put(candidateEntity, hwvtepConnectionInstance);
        hwvtepConnectionInstance.setConnectedEntity(candidateEntity);

        try {
            Registration registration = entityOwnershipService.registerCandidate(candidateEntity);
            hwvtepConnectionInstance.setDeviceOwnershipCandidateRegistration(registration);
            LOG.info("HWVTEP entity {} is registered for ownership.", candidateEntity);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB entity {} was already registered for ownership", candidateEntity, e);
        }
        handleOwnershipState(candidateEntity, hwvtepConnectionInstance);
    }

    private void handleOwnershipState(final Entity candidateEntity,
            final HwvtepConnectionInstance hwvtepConnectionInstance) {
        //If entity already has owner, it won't get notification from EntityOwnershipService
        //so cache the connection instances.
        Optional<EntityOwnershipState> ownershipStateOpt = entityOwnershipService.getOwnershipState(candidateEntity);
        if (ownershipStateOpt.isPresent()) {
            EntityOwnershipState ownershipState = ownershipStateOpt.orElseThrow();
            putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(), hwvtepConnectionInstance);
            if (ownershipState != EntityOwnershipState.NO_OWNER) {
                hwvtepConnectionInstance.setHasDeviceOwnership(ownershipState == EntityOwnershipState.IS_OWNER);
                if (ownershipState != EntityOwnershipState.IS_OWNER) {
                    LOG.info("HWVTEP entity {} is already owned by other southbound plugin "
                                    + "instance, so *this* instance is NOT an OWNER of the device",
                            hwvtepConnectionInstance.getConnectionInfo());
                } else {
                    registerCallbacks(hwvtepConnectionInstance);
                }
            }
        }
    }

    private static Global getHwvtepGlobalTableEntry(final HwvtepConnectionInstance connectionInstance) {
        final TypedDatabaseSchema dbSchema;
        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
            return null;
        }

        GenericTableSchema hwvtepSchema = dbSchema.getTableSchema(Global.class);
        Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
        selectOperation.setColumns(hwvtepSchema.getColumnList());

        ArrayList<Operation> operations = new ArrayList<>(2);
        operations.add(selectOperation);
        operations.add(op.comment("Fetching hardware_vtep table rows"));

        final List<OperationResult> results;
        try {
            results = connectionInstance.transact(dbSchema, operations).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch hardware_vtep table row from device {}", connectionInstance.getConnectionInfo(),
                e);
            return null;
        }

        final Global globalRow;
        if (results != null) {
            OperationResult selectResult = results.get(0);
            globalRow = TyperUtils.getTypedRowWrapper(dbSchema,Global.class,selectResult.getRows().get(0));
        } else {
            globalRow = null;
        }
        LOG.info("Fetched global {} from hardware_vtep schema", globalRow);
        return globalRow;
    }

    private Entity getEntityFromConnectionInstance(@NonNull final HwvtepConnectionInstance hwvtepConnectionInstance) {
        InstanceIdentifier<Node> iid = hwvtepConnectionInstance.getInstanceIdentifier();
        if (iid == null) {
            //TODO: Is Global the right one?
            Global hwvtepGlobalRow = getHwvtepGlobalTableEntry(hwvtepConnectionInstance);
            iid = HwvtepSouthboundMapper.getInstanceIdentifier(hwvtepGlobalRow);
            /* Let's set the iid now */
            hwvtepConnectionInstance.setInstanceIdentifier(iid);
            LOG.trace("InstanceIdentifier {} generated for device "
                    + "connection {}",iid, hwvtepConnectionInstance.getConnectionInfo());
            controllerTxHistory.putIfAbsent(iid,
                    new TransactionHistory(TRANSACTION_HISTORY_CAPACITY, TRANSACTION_HISTORY_WATERMARK));
            deviceUpdateHistory.putIfAbsent(iid,
                    new TransactionHistory(TRANSACTION_HISTORY_CAPACITY, TRANSACTION_HISTORY_WATERMARK));
            TransactionHistory controllerLog = controllerTxHistory.get(iid);
            TransactionHistory deviceLog = deviceUpdateHistory.get(iid);
            int port = hwvtepConnectionInstance.getOvsdbClient().getConnectionInfo().getRemotePort();
            deviceLog.addToHistory(TransactionType.ADD, new ClientConnected(port));
            LOG.info("CONTROLLER - {} {}", TransactionType.ADD, new ClientConnected(port));
            hwvtepConnectionInstance.setControllerTxHistory(controllerLog);
            hwvtepConnectionInstance.setDeviceUpdateHistory(deviceLog);
        }
        Entity deviceEntity = new Entity(ENTITY_TYPE, iid);
        LOG.debug("Entity {} created for device connection {}",
                deviceEntity, hwvtepConnectionInstance.getConnectionInfo());
        return deviceEntity;
    }

    private void unregisterEntityForOwnership(final HwvtepConnectionInstance hwvtepConnectionInstance) {
        hwvtepConnectionInstance.closeDeviceOwnershipCandidateRegistration();
        entityConnectionMap.remove(hwvtepConnectionInstance.getConnectedEntity());
    }

    public void reconcileConnection(final InstanceIdentifier<Node> iid, final HwvtepGlobalAugmentation hwvtepNode) {
        retryConnection(iid, hwvtepNode,
                ConnectionReconciliationTriggers.ON_CONTROLLER_INITIATED_CONNECTION_FAILURE);
    }

    public void stopConnectionReconciliationIfActive(final InstanceIdentifier<?> iid,
            final HwvtepGlobalAugmentation hwvtepNode) {
        final ReconciliationTask task = new ConnectionReconciliationTask(
                reconciliationManager,
                this,
                iid,
                hwvtepNode);
        reconciliationManager.dequeue(task);
    }

    private void retryConnection(final InstanceIdentifier<Node> iid, final HwvtepGlobalAugmentation hwvtepNode,
                                 final ConnectionReconciliationTriggers trigger) {
        if (hwvtepNode == null) {
            //switch initiated connection
            return;
        }
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
            case ON_DISCONNECT: {
                ReadTransaction tx = db.newReadOnlyTransaction();
                FluentFuture<Optional<Node>> readNodeFuture =
                        tx.read(LogicalDatastoreType.CONFIGURATION, iid);

                readNodeFuture.addCallback(new FutureCallback<Optional<Node>>() {
                    @Override
                    public void onSuccess(final Optional<Node> node) {
                        if (node.isPresent()) {
                            HwvtepGlobalAugmentation augmentation = node.orElseThrow()
                                    .augmentation(HwvtepGlobalAugmentation.class);
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
                    public void onFailure(final Throwable ex) {
                        LOG.warn("Read Config/DS for Node failed! {}", iid, ex);
                    }
                }, MoreExecutors.directExecutor());
                break;
            }
            default:
                break;
        }
    }

    public void handleOwnershipChanged(final Entity entity, final EntityOwnershipStateChange change) {
        HwvtepConnectionInstance hwvtepConnectionInstance = getConnectionInstanceFromEntity(entity);
        LOG.info("handleOwnershipChanged: {} event received for device {}",
            change, hwvtepConnectionInstance != null ? hwvtepConnectionInstance.getConnectionInfo()
                        : "THAT'S NOT REGISTERED BY THIS SOUTHBOUND PLUGIN INSTANCE");

        if (hwvtepConnectionInstance == null) {
            if (change.isOwner()) {
                LOG.warn("handleOwnershipChanged: found no connection instance for {}", entity);
            } else {
                // EntityOwnershipService sends notification to all the nodes, irrespective of whether
                // that instance registered for the device ownership or not. It is to make sure that
                // If all the controller instance that was connected to the device are down, so the
                // running instance can clear up the operational data store even though it was not
                // connected to the device.
                LOG.debug("handleOwnershipChanged: found no connection instance for {}", entity);
            }

            // If entity has no owner, clean up the operational data store (it's possible because owner controller
            // might went down abruptly and didn't get a chance to clean up the operational data store.
            if (!change.hasOwner()) {
                LOG.info("{} has no owner, cleaning up the operational data store", entity);
                // Below code might look weird but it's required. We want to give first opportunity to the
                // previous owner of the device to clean up the operational data store if there is no owner now.
                // That way we will avoid lot of nasty md-sal exceptions because of concurrent delete.
                InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>) entity.getIdentifier();
                hwvtepOperGlobalListener.scheduleOldConnectionNodeDelete(nodeIid);
                /*
                Assuming node1 was the owner earlier.
                If the owner relinquished he would have cleaned it already in which case the above would be a no op
                If the owner crashed then the above would clean the node after the scheduled delay
                The live nodes (two and three) will try to cleanup but that is ok one of them ends up cleaning.
                But if the southbound connects again that connection can itself trigger the pending cleanup and
                the above op would become noop again.

                In summary
                In The following cases it would be a noop
                1) The southbound connects again within the scheduled cleanup delay.
                2) The owner node1 which is not crashed cleaned the node properly.

                In the following case both node2 and node3 will try to clean it (one of them will succeed ).
                 1) node1 which was the owner crashed
                 */
            }
            return;
        }
        //Connection detail need to be cached, irrespective of ownership result.
        putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(), hwvtepConnectionInstance);

        if (change.isOwner() == hwvtepConnectionInstance.getHasDeviceOwnership()) {
            LOG.debug("handleOwnershipChanged: no change in ownership for {}. Ownership status is : {}",
                    hwvtepConnectionInstance.getConnectionInfo(), hwvtepConnectionInstance.getHasDeviceOwnership());
            return;
        }

        hwvtepConnectionInstance.setHasDeviceOwnership(change.isOwner());
        // You were not an owner, but now you are
        if (change.isOwner()) {
            LOG.info("handleOwnershipChanged: *this* southbound plugin instance is owner of device {}",
                    hwvtepConnectionInstance.getConnectionInfo());

            //*this* instance of southbound plugin is owner of the device,
            //so register for monitor callbacks
            registerCallbacks(hwvtepConnectionInstance);

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

    private HwvtepConnectionInstance getConnectionInstanceFromEntity(final Entity entity) {
        return entityConnectionMap.get(entity);
    }

    private static final class HwvtepDeviceEntityOwnershipListener implements EntityOwnershipListener {
        private final HwvtepConnectionManager hcm;
        private final Registration listenerRegistration;

        HwvtepDeviceEntityOwnershipListener(final HwvtepConnectionManager hcm,
                final EntityOwnershipService entityOwnershipService) {
            this.hcm = hcm;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(final Entity entity, final EntityOwnershipStateChange change,
                final boolean inJeopardy) {
            hcm.handleOwnershipChanged(entity, change);
        }
    }

    final Map<InstanceIdentifier<Node>, HwvtepDeviceInfo> allConnectedInstances() {
        return Maps.transformValues(Collections.unmodifiableMap(nodeIidVsConnectionInstance),
            HwvtepConnectionInstance::getDeviceInfo);
    }

    final Map<InstanceIdentifier<Node>, TransactionHistory> controllerTxHistory() {
        return Collections.unmodifiableMap(controllerTxHistory);
    }

    final Map<InstanceIdentifier<Node>, TransactionHistory> deviceUpdateHistory() {
        return Collections.unmodifiableMap(deviceUpdateHistory);
    }

    public void cleanupOperationalNode(final InstanceIdentifier<Node> nodeIid) {
        txInvoker.invoke(new HwvtepGlobalRemoveCommand(nodeIid));
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
