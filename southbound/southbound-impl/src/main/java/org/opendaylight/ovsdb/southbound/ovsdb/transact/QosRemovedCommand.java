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
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosRemovedCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QosRemovedCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractOriginal(events, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractOriginal(modifications, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originals,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated) {
        for (Map.Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originalEntry : originals
                .entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = originalEntry.getKey();
            OvsdbNodeAugmentation original = originalEntry.getValue();
            OvsdbNodeAugmentation update = updated.get(ovsdbNodeIid);

            if (original != null && update != null) {
                List<QosEntries> origQosEntries = original.getQosEntries();
                List<QosEntries> updatedQosEntries = update.getQosEntries();
                if (origQosEntries != null && !origQosEntries.isEmpty()) {
                    for (QosEntries origQosEntry : origQosEntries) {
                        OvsdbNodeAugmentation operNode =
                                state.getBridgeNode(ovsdbNodeIid).get().augmentation(OvsdbNodeAugmentation.class);
                        List<QosEntries> operQosEntries = operNode.getQosEntries();

                        boolean found = false;
                        if (updatedQosEntries != null && !updatedQosEntries.isEmpty()) {
                            for (QosEntries updatedQosEntry : updatedQosEntries) {
                                if (origQosEntry.getQosId().equals(updatedQosEntry.getQosId())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            LOG.debug("Received request to delete QoS entry {}", origQosEntry.getQosId());
                            Uuid qosUuid = getQosEntryUuid(operQosEntries, origQosEntry.getQosId());
                            if (qosUuid != null) {
                                Qos qos = transaction.getTypedRowSchema(Qos.class);
                                transaction.add(op.delete(qos.getSchema())
                                        .where(qos.getUuidColumn().getSchema().opEqual(new UUID(qosUuid.getValue())))
                                        .build());
                                LOG.info("Removed QoS Uuid : {} for node : {} ",
                                        origQosEntry.getQosId(), ovsdbNodeIid);
                            } else {
                                LOG.warn("Unable to delete QoS{} for node {} because it was not found in the "
                                        + "operational store, and thus we cannot retrieve its UUID",
                                        origQosEntry.getQosId(), ovsdbNodeIid);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Uuid getQosEntryUuid(final List<QosEntries> operQosEntries, final Uri qosId) {
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
