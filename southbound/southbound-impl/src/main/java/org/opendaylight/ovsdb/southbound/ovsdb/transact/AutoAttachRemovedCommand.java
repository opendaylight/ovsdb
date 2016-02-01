/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
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

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoAttachRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachRemovedCommand.class);

    public AutoAttachRemovedCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> original =
                TransactUtils.extractOriginal(getChanges(), OvsdbNodeAugmentation.class);

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated =
                TransactUtils.extractUpdated(getChanges(), OvsdbNodeAugmentation.class);

        Iterator<InstanceIdentifier<OvsdbNodeAugmentation>> itr = original.keySet().iterator();
        while (itr.hasNext()) {
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = itr.next();
            OvsdbNodeAugmentation ovsdbNodeAugmentation = original.get(ovsdbNodeIid);
            OvsdbNodeAugmentation updatedOvsdbNodeAugmentation = updated.get(ovsdbNodeIid);

            if (ovsdbNodeAugmentation != null && updatedOvsdbNodeAugmentation != null) {
                List<Autoattach> origAutoattachList = ovsdbNodeAugmentation.getAutoattach();
                List<Autoattach> updatedAutoattachList = updatedOvsdbNodeAugmentation.getAutoattach();
                if (origAutoattachList != null && !origAutoattachList.isEmpty()) {
                    for (Autoattach origAutoattach : origAutoattachList) {
                        OvsdbNodeAugmentation currentOvsdbNode =
                                getOperationalState().getBridgeNode(ovsdbNodeIid).get().getAugmentation(OvsdbNodeAugmentation.class);
                        List<Autoattach> currentAutoAttach = currentOvsdbNode.getAutoattach();

                        boolean found = false;
                        if (updatedAutoattachList != null && !updatedAutoattachList.isEmpty()) {
                            for (Autoattach updatedAutoattach : updatedAutoattachList) {
                                if (origAutoattach.getAutoattachId().equals(updatedAutoattach.getAutoattachId())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            LOG.debug("Received request to delete Autoattach entry {}", origAutoattach.getAutoattachId());
                            Uuid autoattachUuid = getAutoAttachUuid(currentAutoAttach, origAutoattach.getAutoattachId());
                            if (autoattachUuid != null) {
                                AutoAttach autoattach = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), AutoAttach.class, null);
                                  transaction.add(op.delete(autoattach.getSchema())
                                          .where(autoattach.getUuidColumn().getSchema().opEqual(new UUID(autoattachUuid.getValue())))
                                          .build());
                            } else {
                                LOG.warn("Unable to delete AutoAttach {} for node {} because it was not found in the operational store, "
                                        + "and thus we cannot retrieve its UUID", origAutoattach.getAutoattachId(), ovsdbNodeIid);
                            }
                        }
                    }
                }
            }
        }
    }

    private Uuid getAutoAttachUuid(List<Autoattach> currentAutoAttach, Uri autoAttachId) {
        if (currentAutoAttach != null && !currentAutoAttach.isEmpty()) {
            for (Autoattach autoAttach : currentAutoAttach) {
                if (autoAttach.getAutoattachUuid().equals(autoAttachId)) {
                    return autoAttach.getAutoattachUuid();
                }
            }
        }
        return null;
    }
}
