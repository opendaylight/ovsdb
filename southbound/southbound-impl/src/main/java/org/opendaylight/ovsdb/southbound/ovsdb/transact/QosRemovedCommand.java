/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class QosRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QosRemovedCommand.class);

    public QosRemovedCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Set<InstanceIdentifier<QosEntries>> removed =
                TransactUtils.extractRemoved(getChanges(),QosEntries.class);

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originals
            = TransactUtils.extractOriginal(getChanges(),OvsdbNodeAugmentation.class);

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated
        = TransactUtils.extractUpdated(getChanges(), OvsdbNodeAugmentation.class);

        Iterator<InstanceIdentifier<OvsdbNodeAugmentation>> itr = originals.keySet().iterator();
        while (itr.hasNext()) {
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = itr.next();
            OvsdbNodeAugmentation original = originals.get(ovsdbNodeIid);
            OvsdbNodeAugmentation update = updated.get(ovsdbNodeIid);

            if (original != null && update != null) {
                List<QosEntries> origQosEntries = original.getQosEntries();
                List<QosEntries> updatedQosEntries = update.getQosEntries();
                if (origQosEntries != null && !origQosEntries.isEmpty()) {
                    for (QosEntries origQosEntry : origQosEntries) {
                        OvsdbNodeAugmentation operNode = getOperationalState().getBridgeNode(ovsdbNodeIid).get().getAugmentation(OvsdbNodeAugmentation.class);
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
                            LOG.info("Received request to delete QoS entry {}",origQosEntry.getQosId());
                            Uuid qosUuid = getQosEntryUuid(operQosEntries, origQosEntry.getQosId());
                            if (qosUuid != null) {
                                UUID uuid = new UUID(qosUuid.getValue());
                                Qos qos = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Qos.class, null);
                                  transaction.add(op.delete(qos.getSchema())
                                          .where(qos.getUuidColumn().getSchema().opEqual(uuid))
                                          .build());
                            }
                        }
                    }
                }
            }
/*            } else {
                LOG.warn("Unable to delete QoS{} because it was not found in the operational store, "
                        + "and thus we cannot retrieve its UUID", ovsdbNodeIid);
            }
*/
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
