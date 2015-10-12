/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class HwvtepConnectionManager implements OvsdbConnectionListener, AutoCloseable{
    private Map<ConnectionInfo, HwvtepConnectionInstance> clients =
                    new ConcurrentHashMap<ConnectionInfo,HwvtepConnectionInstance>();
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepConnectionManager.class);
    private static final String ENTITY_TYPE = "hwvtep";

    private DataBroker db;
    private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers =
                    new ConcurrentHashMap<ConnectionInfo,InstanceIdentifier<Node>>();
    private Map<Entity, HwvtepConnectionInstance> entityConnectionMap =
                    new ConcurrentHashMap<>();
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
        // TODO Auto-generated method stub
        
    }
    @Override
    public void connected(@Nonnull final OvsdbClient client) {
        HwvtepConnectionInstance hwClient = connectedButCallBacksNotRegistered(client);
        registerEntityForOwnership(hwClient);
        LOG.trace("connected client: {}", client);
    }

    @Override
    public void disconnected(OvsdbClient client) {
        // TODO Auto-generated method stub
        LOG.trace("disconnected client: {}", client);
    }

    public HwvtepConnectionInstance connectedButCallBacksNotRegistered(final OvsdbClient externalClient) {
        LOG.info("OVSDB Connection from {}:{}",externalClient.getConnectionInfo().getRemoteAddress(),
                externalClient.getConnectionInfo().getRemotePort());
        ConnectionInfo key = HwvtepSouthboundMapper.createConnectionInfo(externalClient);
        HwvtepConnectionInstance hwvtepConnectionInstance = getConnectionInstance(key);

        // Check if existing ovsdbConnectionInstance for the OvsdbClient present.
        // In such cases, we will see if the ovsdbConnectionInstance has same externalClient.
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

    public HwvtepConnectionInstance getConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        return clients.get(connectionInfo);
    }

    public InstanceIdentifier<Node> getInstanceIdentifier(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        InstanceIdentifier<Node> iid = instanceIdentifiers.get(connectionInfo);
        return iid;
    }

    private void removeConnectionInstance(ConnectionInfo key) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.remove(connectionInfo);
    }

    private void putConnectionInstance(ConnectionInfo key,HwvtepConnectionInstance instance) {
        ConnectionInfo connectionInfo = HwvtepSouthboundMapper.suppressLocalIpPort(key);
        clients.put(connectionInfo, instance);
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

            List<String> hwvtepTableColumn = new ArrayList<String>();
            hwvtepTableColumn.addAll(hwvtepSchema.getColumns());
            Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
            selectOperation.setColumns(hwvtepTableColumn);;

            ArrayList<Operation> operations = new ArrayList<Operation>();
            operations.add(selectOperation);
            operations.add(op.comment("Fetching hardware_vtep table rows"));
            List<OperationResult> results = null;
            try {
                results = connectionInstance.transact(dbSchema, operations).get();
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
        return globalRow;
    }

    private Entity getEntityFromConnectionInstance(@Nonnull HwvtepConnectionInstance hwvtepConnectionInstance) {
        YangInstanceIdentifier entityId = null;
        InstanceIdentifier<Node> iid = hwvtepConnectionInstance.getInstanceIdentifier();;
        if ( iid == null ) {
            //TODO: Is Global the right one?
            Global hwvtepGlobalRow = getHwvtepGlobalTableEntry(hwvtepConnectionInstance);
            iid = HwvtepSouthboundMapper.getInstanceIdentifier(hwvtepGlobalRow);
            LOG.info("InstanceIdentifier {} generated for device "
                    + "connection {}",iid, hwvtepConnectionInstance.getConnectionInfo());

        }
        entityId = HwvtepSouthboundUtil.getInstanceIdentifierCodec().getYangInstanceIdentifier(iid);
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
        // TODO Auto-generated method stub
        LOG.error("CODE INCOMPLETE");
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
