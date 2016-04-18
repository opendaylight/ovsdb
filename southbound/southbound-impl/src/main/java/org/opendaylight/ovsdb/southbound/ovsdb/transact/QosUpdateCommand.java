/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class QosUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QosUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        Collection<DataTreeModification<Node>> modifications) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> createdOrUpdated) {
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry:
            createdOrUpdated.entrySet()) {
            updateQos(transaction, state, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
    }

    private void updateQos(
            TransactionBuilder transaction, BridgeOperationalState state,
            InstanceIdentifier<OvsdbNodeAugmentation> iid, OvsdbNodeAugmentation ovsdbNode) {

        List<QosEntries> qosEntries = ovsdbNode.getQosEntries();

        if (!state.getBridgeNode(iid).isPresent()) {
            return;
        }
        OvsdbNodeAugmentation operNode = state.getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);
        List<QosEntries> operQosEntries = operNode.getQosEntries();

        if (qosEntries != null) {
            for (QosEntries qosEntry : qosEntries) {
                Qos qos = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Qos.class);

                if (qosEntry.getQosType() != null) {
                    qos.setType(SouthboundMapper.createQosType(qosEntry.getQosType()));
                }

                Uuid qosUuid = getQosEntryUuid(operQosEntries, qosEntry.getQosId());
                UUID uuid = null;
                if (qosUuid != null) {
                    uuid = new UUID(qosUuid.getValue());
                }

                List<QueueList> queueList = qosEntry.getQueueList();
                Map<Long, UUID>newQueueList = new HashMap<>();
                if (queueList != null && !queueList.isEmpty()) {
                    for (QueueList queue : queueList) {
                        newQueueList.put(queue.getQueueNumber(), new UUID(queue.getQueueUuid().getValue()));
                    }
                }
                qos.setQueues(newQueueList);

                List<QosExternalIds> externalIds = qosEntry.getQosExternalIds();
                Map<String, String> externalIdsMap = new HashMap<>();
                if (externalIds != null) {
                    for (QosExternalIds externalId : externalIds) {
                        externalIdsMap.put(externalId.getQosExternalIdKey(), externalId.getQosExternalIdValue());
                    }
                }
                externalIdsMap.put(SouthboundConstants.QOS_ID_EXTERNAL_ID_KEY, qosEntry.getQosId().getValue());
                try {
                    qos.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete Qos external IDs", e);
                }

                List<QosOtherConfig> otherConfigs = qosEntry.getQosOtherConfig();
                if (otherConfigs != null) {
                    Map<String, String> otherConfigsMap = new HashMap<>();
                    for (QosOtherConfig otherConfig : otherConfigs) {
                        otherConfigsMap.put(otherConfig.getOtherConfigKey(), otherConfig.getOtherConfigValue());
                    }
                    try {
                        qos.setOtherConfig(ImmutableMap.copyOf(otherConfigsMap));
                    } catch (NullPointerException e) {
                        LOG.warn("Incomplete Qos other_config", e);
                    }
                }
                if (uuid == null) {
                    transaction.add(op.insert(qos)).build();
                } else {
                    Qos extraQos = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), Qos.class, null);
                    extraQos.getUuidColumn().setData(uuid);
                    transaction.add(op.update(qos)
                            .where(extraQos.getUuidColumn().getSchema().opEqual(uuid)).build());
                }
                transaction.build();
            }
        }
    }

    private Uuid getQosEntryUuid(List<QosEntries> operQosEntries, Uri qosId) {
        if (operQosEntries != null && !operQosEntries.isEmpty()) {
            for (QosEntries qosEntry : operQosEntries) {
                if (qosEntry.getQosId().equals(qosId)) {
                    return qosEntry.getQosUuid();
                }
            }
        }
        return null;
    }
}
