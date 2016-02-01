/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.Mappings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;

public class AutoAttachUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachUpdateCommand.class);

    public AutoAttachUpdateCommand(BridgeOperationalState state, AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> created =
                TransactUtils.extractCreated(getChanges(), OvsdbNodeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry
                : created.entrySet()) {
            LOG.debug("AutoAttach table create request");
            updateAutoAttach(transaction, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated =
                TransactUtils.extractUpdated(getChanges(), OvsdbNodeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry
                : updated.entrySet()) {
            LOG.debug("AutoAttach table update request");
            updateAutoAttach(transaction, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
    }

    private void updateAutoAttach(TransactionBuilder transaction,
            InstanceIdentifier<OvsdbNodeAugmentation> iid,
            OvsdbNodeAugmentation ovsdbNode) {

        List<Autoattach> autoAttachList = ovsdbNode.getAutoattach();
        if (!getOperationalState().getBridgeNode(iid).isPresent()) {
            return;
        }
        OvsdbNodeAugmentation currentOvsdbNode = getOperationalState().getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);
        List<Autoattach> currentAutoAttach = currentOvsdbNode.getAutoattach();

        if (autoAttachList != null) {
            for (Autoattach autoAttach : autoAttachList) {
                AutoAttach autoAttachWrapper = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), AutoAttach.class);
                if (autoAttach.getSystemName() != null) {
                    autoAttachWrapper.setSystemName(autoAttach.getSystemName());
                    LOG.info("AutoAttachUpdateCommand updating system name {}", autoAttach.getSystemName());
                }
                if (autoAttach.getSystemDescription() != null) {
                    autoAttachWrapper.setSystemDescription(autoAttach.getSystemDescription());
                    LOG.info("AutoAttachUpdateCommand updating system description {}", autoAttach.getSystemDescription());
                }

                List<Mappings> mappingsList = autoAttach.getMappings();
                Map<Long, Long> newMappings = new HashMap<>();
                if (mappingsList != null && !mappingsList.isEmpty()) {
                    for (Mappings mappings : mappingsList) {
                        Long mappingsValue = new Long(mappings.getMappingsValue().toString());
                        newMappings.put(mappings.getMappingsKey(), mappingsValue);
                        LOG.info("AutoAttachUpdateCommand updating mappings {} {}", mappings.getMappingsKey(), mappingsValue);
                    }
                    autoAttachWrapper.setMappings(newMappings);
                }

                Uuid autoAttachUuid = getAutoAttachUuid(currentAutoAttach,
                        new Uuid (autoAttach.getAutoattachId().getValue().replaceAll(SouthboundConstants.AUTOATTACH_URI_PREFIX + "://", "")));
                UUID uuid = null;
                if (autoAttachUuid != null) {
                    uuid = new UUID(autoAttachUuid.getValue());
                    AutoAttach newAutoAttach = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), AutoAttach.class, null);
                    newAutoAttach.getUuidColumn().setData(uuid);
                    LOG.info("Updating autoattach table entries {}", uuid);
                    transaction.add(op.update(autoAttachWrapper)
                            .where(newAutoAttach.getUuidColumn().getSchema().opEqual(uuid)).build());
                } else {
                    Uri bridgeUri = autoAttach.getBridgeId();
                    String aaUuid = SouthboundMapper.getRandomUUID();
                    Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                    transaction.add(op.insert(autoAttachWrapper).withId(aaUuid));
                    if (getBridge(iid, bridgeUri) != null) {
                        bridge.setName(getBridge(iid, bridgeUri).getBridgeName().getValue());
                        bridge.setAutoAttach(Sets.newHashSet(new UUID(aaUuid)));
                        LOG.info("Create Autoattach table {}, "
                                + "and mutate the bridge", aaUuid, getBridge(iid, bridgeUri).getBridgeName().getValue());
                        transaction.add(op.mutate(bridge)
                                .addMutation(bridge.getAutoAttachColumn().getSchema(),
                                        Mutator.INSERT,bridge.getAutoAttachColumn().getData())
                                .where(bridge.getNameColumn().getSchema()
                                        .opEqual(bridge.getNameColumn().getData())).build());
                    }
                }
                transaction.build();
            }
        }
    }

    private OvsdbBridgeAugmentation getBridge(InstanceIdentifier<OvsdbNodeAugmentation> key,
            Uri bridgeUri) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(bridgeUri)))
                .augmentation(OvsdbBridgeAugmentation.class);

        OvsdbBridgeAugmentation bridge = null;
        ReadOnlyTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<OvsdbBridgeAugmentation>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        try {
            Optional<OvsdbBridgeAugmentation> bridgeOptional = future.get();
            if (bridgeOptional.isPresent()) {
                bridge = bridgeOptional.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("Error reading from datastore",e);
        }
        transaction.close();
        return bridge;
    }

    private Uuid getAutoAttachUuid(List<Autoattach> currentAutoAttach, Uuid autoattachUuid) {
        LOG.info("Looking for aaUuid {}", autoattachUuid.getValue());
        if (currentAutoAttach != null && !currentAutoAttach.isEmpty()) {
            for (Autoattach autoAttach : currentAutoAttach) {
                if (autoAttach.getAutoattachUuid().equals(autoattachUuid)) {
                    return autoAttach.getAutoattachUuid();
                }
            }
        }
        return null;
    }
}
