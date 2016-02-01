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

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.Mappings;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class AutoAttachUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachUpdateCommand.class);

    public AutoAttachUpdateCommand(BridgeOperationalState state, AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {

        LOG.info("In AutoAttachUpdateCommand");
        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> created =
                TransactUtils.extractCreated(getChanges(), OvsdbNodeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry
                : created.entrySet()) {
            updateAutoAttach(transaction, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated =
                TransactUtils.extractUpdated(getChanges(), OvsdbNodeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry
                : updated.entrySet()) {
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
                Map<Long, Integer> newMappings = new HashMap<>();
                if (mappingsList != null && !mappingsList.isEmpty()) {
                    for (Mappings mappings : mappingsList) {
                        newMappings.put(mappings.getMappingsKey(), mappings.getMappingsValue());
                        LOG.info("AutoAttachUpdateCommand updating mappings {} {}", mappings.getMappingsKey(), mappings.getMappingsValue());
                    }
                }
                autoAttachWrapper.setMappings(newMappings);

                Uuid autoAttachUuid = getAutoAttachUuid(currentAutoAttach, autoAttach.getAutoattachUuid());
                UUID uuid = null;
                if (autoAttachUuid != null) {
                    uuid = new UUID(autoAttachUuid.getValue());
                    AutoAttach newAutoAttach = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), AutoAttach.class, null);
                    newAutoAttach.getUuidColumn().setData(uuid);
                    transaction.add(op.update(autoAttachWrapper)
                            .where(newAutoAttach.getUuidColumn().getSchema().opEqual(uuid)).build());
                } else {
                    transaction.add(op.insert(autoAttachWrapper)).build();
                }
                transaction.build();
            }
        }
    }

    private Uuid getAutoAttachUuid(List<Autoattach> currentAutoAttach, Uuid autoattachUuid) {
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
