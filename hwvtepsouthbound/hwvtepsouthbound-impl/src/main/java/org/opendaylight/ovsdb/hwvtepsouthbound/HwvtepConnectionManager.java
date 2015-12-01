/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class HwvtepConnectionManager implements OvsdbConnectionListener, AutoCloseable{
    private Map<ConnectionInfo, HwvtepConnectionInstance> clients = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepConnectionManager.class);
    private static final String ENTITY_TYPE = "hwvtep";

    private DataBroker db;
    private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers = new ConcurrentHashMap<>();
    private Map<Entity, HwvtepConnectionInstance> entityConnectionMap = new ConcurrentHashMap<>();
    private EntityOwnershipService entityOwnershipService;
    private HwvtepDeviceEntityOwnershipListener hwvtepDeviceEntityOwnershipListener;

    public HwvtepConnectionManager(DataBroker db, TransactionInvoker txInvoker,
                    EntityOwnershipService entityOwnershipService) {
        this.db = db;
        this.txInvoker = txInvoker;
        this.entityOwnershipService = entityOwnershipService;
        this.hwvtepDeviceEntityOwnershipListener = new HwvtepDeviceEntityOwnershipListener(this,entityOwnershipService);
    }

    @Override
    public void close() throws Exception {
        if (hwvtepDeviceEntityOwnershipListener != null) {
            hwvtepDeviceEntityOwnershipListener.close();
        }

        for (OvsdbClient client: clients.values()) {
            client.disconnect();
        }
    }

    @Override
    public void connected(@Nonnull final OvsdbClient client) {
        HwvtepConnectionInstance hwClient = connectedButCallBacksNotRegistered(client);
        registerEntityForOwnership(hwClient);
        LOG.trace("connected client: {}", client);
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("HWVTEP Disconnected from {}:{}. Cleaning up the operational data store"
                        ,client.getConnectionInfo().getRemoteAddress(),
                        client.getConnectionInfo().getRemotePort());
        ConnectionInfo key = HwvtepSouthboundMapper.createConnectionInfo(client);
        HwvtepConnectionInstance hwvtepConnectionInstance = getConnectionInstance(key);
        if (hwvtepConnectionInstance != null) {
            //TODO: remove all the hwvtep nodes
            txInvoker.invoke(new HwvtepGlobalRemoveCommand(hwvtepConnectionInstance, null, null));
            removeConnectionInstance(key);

            // Unregister Cluster Ownership for ConnectionInfo
            unregisterEntityForOwnership(hwvtepConnectionInstance);
        } else {
            LOG.warn("HWVTEP disconnected event did not find connection instance for {}", key);
        }
        LOG.trace("disconnected client: {}", client);
    }

    public OvsdbClient connect(InstanceIdentifier<Node> iid, HwvtepGlobalAugmentation hwvtepGlobal) throws UnknownHostException {
        InetAddress ip = HwvtepSouthboundMapper.createInetAddress(hwvtepGlobal.getConnectionInfo().getRemoteIp());
        OvsdbClient client = OvsdbConnectionService.getService()
                        .connect(ip, hwvtepGlobal.getConnectionInfo().getRemotePort().getValue());
        if(client != null) {
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

        hwvtepConnectionInstance = new HwvtepConnectionInstance(key, externalClient, getInstanceIdentifier(key),
                txInvoker);
        hwvtepConnectionInstance.createTransactInvokers();
        return hwvtepConnectionInstance;
    }

    private void putConnectionInstance(ConnectionInfo key,HwvtepConnectionInstance instance) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
        LOG.info("Clients after put: {}", clients);
    }

    public HwvtepConnectionInstance getConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
    }

    public HwvtepConnectionInstance getConnectionInstance(Node node) {
        Preconditions.checkNotNull(node);
        HwvtepGlobalAugmentation hwvtepGlobal = node.getAugmentation(HwvtepGlobalAugmentation.class);
        PhysicalSwitchAugmentation pSwitchNode = node.getAugmentation(PhysicalSwitchAugmentation.class);
        if (hwvtepGlobal != null) {
            if(hwvtepGlobal.getConnectionInfo() != null) {
                return getConnectionInstance(hwvtepGlobal.getConnectionInfo());
            } else {
                // TODO: Case of user configured connection
                return null; //for now
            }
        } //TODO: We could get it from Managers also.
        else if(pSwitchNode != null){
            return getConnectionInstance(pSwitchNode);
        } else {
            LOG.warn("This is not a node that gives any hint how to find its OVSDB Manager: {}",node);
            return null;
        }
    }

    private HwvtepConnectionInstance getConnectionInstance(HwvtepPhysicalSwitchAttributes pNode) {
        Optional<HwvtepGlobalAugmentation> optional = HwvtepSouthboundUtil.getManagingNode(db, pNode);
        if(optional.isPresent()) {
            return getConnectionInstance(optional.get().getConnectionInfo());
        } else {
            return null;
        }
    }

    private void removeConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.remove(connectionInfo);
        LOG.info("Clients after remove: {}", clients);
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
        return getConnectionInstance(connectionInfo);
    }

    private void registerEntityForOwnership(HwvtepConnectionInstance hwvtepConnectionInstance) {

        Entity candidateEntity = getEntityFromConnectionInstance(hwvtepConnectionInstance);
        entityConnectionMap.put(candidateEntity, hwvtepConnectionInstance);
        hwvtepConnectionInstance.setConnectedEntity(candidateEntity);

        try {
            EntityOwnershipCandidateRegistration registration =
                    entityOwnershipService.registerCandidate(candidateEntity);
            hwvtepConnectionInstance.setDeviceOwnershipCandidateRegistration(registration);
            LOG.info("HWVTEP entity {} is registered for ownership.", candidateEntity);

            //If entity already has owner, it won't get notification from EntityOwnershipService
            //so cache the connection instances.
            Optional<EntityOwnershipState> ownershipStateOpt =
                    entityOwnershipService.getOwnershipState(candidateEntity);
            if (ownershipStateOpt.isPresent()) {
                EntityOwnershipState ownershipState = ownershipStateOpt.get();
                if (ownershipState.hasOwner() && !ownershipState.isOwner()) {
                    if (getConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo()) != null) {
                        LOG.info("OVSDB entity {} is already owned by other southbound plugin "
                                + "instance, so *this* instance is NOT an OWNER of the device",
                                hwvtepConnectionInstance.getConnectionInfo());
                        putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(),hwvtepConnectionInstance);
                    }
                }
            }
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB entity {} was already registered for {} ownership", candidateEntity, e);
        }

    }

    private Global getHwvtepGlobalTableEntry(HwvtepConnectionInstance connectionInstance) {
        DatabaseSchema dbSchema = null;
        Global globalRow = null;

        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.databaseName).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    HwvtepSchemaConstants.databaseName,connectionInstance.getConnectionInfo(),e);
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
                if (results != null ) {
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
        if ( iid == null ) {
            //TODO: Is Global the right one?
            Global hwvtepGlobalRow = getHwvtepGlobalTableEntry(hwvtepConnectionInstance);
            iid = HwvtepSouthboundMapper.getInstanceIdentifier(hwvtepGlobalRow);
            /* Let's set the iid now */
            hwvtepConnectionInstance.setInstanceIdentifier(iid);
            LOG.info("InstanceIdentifier {} generated for device "
                    + "connection {}",iid, hwvtepConnectionInstance.getConnectionInfo());

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

    public void handleOwnershipChanged(EntityOwnershipChange ownershipChange) {
        HwvtepConnectionInstance hwvtepConnectionInstance = getConnectionInstanceFromEntity(ownershipChange.getEntity());
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
                // Below code might look weird but it's required. We want to give first opportunity to the
                // previous owner of the device to clean up the operational data store if there is no owner now.
                // That way we will avoid lot of nasty md-sal exceptions because of concurrent delete.
                if (ownershipChange.wasOwner()) {
                    cleanEntityOperationalData(ownershipChange.getEntity());
                }
                // If first cleanEntityOperationalData() was called, this call will be no-op.
                cleanEntityOperationalData(ownershipChange.getEntity());
            }
            return;
        }
        //Connection detail need to be cached, irrespective of ownership result.
        putConnectionInstance(hwvtepConnectionInstance.getMDConnectionInfo(),hwvtepConnectionInstance);

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
            hwvtepConnectionInstance.registerCallbacks();

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
        InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>) HwvtepSouthboundUtil
                .getInstanceIdentifierCodec().bindingDeserializer(entity.getId());

        final ReadWriteTransaction transaction = db.newReadWriteTransaction();
        Optional<Node> node = HwvtepSouthboundUtil.readNode(transaction, nodeIid);
        if (node.isPresent()) {
            HwvtepSouthboundUtil.deleteNode(transaction, nodeIid);
        }
    }

    private HwvtepConnectionInstance getConnectionInstanceFromEntity(Entity entity) {
        return entityConnectionMap.get(entity);
    }

    private class HwvtepDeviceEntityOwnershipListener implements EntityOwnershipListener {
        private HwvtepConnectionManager hcm;
        private EntityOwnershipListenerRegistration listenerRegistration;

        HwvtepDeviceEntityOwnershipListener(HwvtepConnectionManager hcm, EntityOwnershipService entityOwnershipService) {
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
}
