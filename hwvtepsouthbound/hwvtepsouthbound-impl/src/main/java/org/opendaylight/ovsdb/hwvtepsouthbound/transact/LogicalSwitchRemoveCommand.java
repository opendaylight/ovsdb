/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class LogicalSwitchRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchRemoveCommand.class);

    public LogicalSwitchRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> removeds =
                extractRemoved(getChanges(),LogicalSwitches.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> created:
                removeds.entrySet()) {
                removeLogicalSwitch(transaction,  created.getKey(), created.getValue());
            }
        }
    }

    private void removeLogicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<LogicalSwitches> lswitchList) {
        for (LogicalSwitches lswitch: lswitchList) {
            LOG.debug("Removing logcial switch named: {}", lswitch.getHwvtepNodeName().getValue());
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(instanceIdentifier, lswitch.getKey());
            LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class, null);

            if (operationalSwitchOptional.isPresent() &&
                    operationalSwitchOptional.get().getLogicalSwitchUuid() != null) {
                UUID logicalSwitchUuid = new UUID(operationalSwitchOptional.get().getLogicalSwitchUuid().getValue());
                transaction.add(op.delete(logicalSwitch.getSchema())
                        .where(logicalSwitch.getUuidColumn().getSchema().opEqual(logicalSwitchUuid)).build());
                transaction.add(op.comment("Logical Switch: Deleting " + lswitch.getHwvtepNodeName().getValue()));
            } else {
                LOG.warn("Unable to delete logical switch {} because it was not found in the operational store",
                        lswitch.getHwvtepNodeName().getValue());
            }
        }
    }

    private Map<InstanceIdentifier<Node>, List<LogicalSwitches>> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<LogicalSwitches> class1) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> result
            = new HashMap<InstanceIdentifier<Node>, List<LogicalSwitches>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                //If the node which logical switches belong to is removed, all logical switches
                //should be removed too.
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    List<LogicalSwitches> lswitchListRemoved = null;
                    if (removed.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        lswitchListRemoved = removed.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    }
                    if (lswitchListRemoved != null) {
                        result.put(key, lswitchListRemoved);
                    }
                }
                //If the node which logical switches belong to is updated, and logical switches may
                //be created or updated or deleted, we need to get deleted ones.
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<LogicalSwitches> lswitchListUpdated = null;
                    List<LogicalSwitches> lswitchListBefore = null;
                    if (updated.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        lswitchListUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    }
                    if (before.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        lswitchListBefore = before.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    }
                    if (lswitchListBefore != null) {
                        List<LogicalSwitches> lswitchListRemoved = new ArrayList<LogicalSwitches>();
                        if (lswitchListUpdated != null) {
                            lswitchListBefore.removeAll(lswitchListUpdated);
                        }
                        //then exclude updated ones
                        for (LogicalSwitches lswitchBefore: lswitchListBefore) {
                            int i = 0;
                            for(; i < lswitchListUpdated.size(); i++) {
                                if (lswitchBefore.getHwvtepNodeName().equals(lswitchListUpdated.get(i).getHwvtepNodeName())) {
                                    break;
                                }
                            }
                            if (i == lswitchListUpdated.size()) {
                                lswitchListRemoved.add(lswitchBefore);
                            }
                        }
                        if (!lswitchListRemoved.isEmpty()) {
                            result.put(key, lswitchListRemoved);
                        }
                    }
                }
            }
        }
        return result;
    }
}
