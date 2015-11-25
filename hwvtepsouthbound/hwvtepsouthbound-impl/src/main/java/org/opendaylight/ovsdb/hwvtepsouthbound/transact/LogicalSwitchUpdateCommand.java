/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
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

public class LogicalSwitchUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchUpdateCommand.class);

    public LogicalSwitchUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> createds =
                extractCreated(getChanges(),LogicalSwitches.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> created:
                createds.entrySet()) {
                updateLogicalSwitch(transaction,  created.getKey(), created.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> updateds =
                extractUpdated(getChanges(),LogicalSwitches.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> updated:
                updateds.entrySet()) {
                updateLogicalSwitch(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateLogicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<LogicalSwitches> lswitchList) {
        for (LogicalSwitches lswitch: lswitchList) {
            LOG.debug("Creating logcial switch named: {}", lswitch.getHwvtepNodeName());
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(instanceIdentifier, lswitch.getKey());
            LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
            setDescription(logicalSwitch, lswitch);
            setTunnelKey(logicalSwitch, lswitch);
            if (!operationalSwitchOptional.isPresent()) {
                setName(logicalSwitch, lswitch, operationalSwitchOptional);
                transaction.add(op.insert(logicalSwitch));
            } else {
                LogicalSwitches updatedLSwitch = operationalSwitchOptional.get();
                String existingLogicalSwitchName = updatedLSwitch.getHwvtepNodeName().getValue();
                // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
                LogicalSwitch extraLogicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
                extraLogicalSwitch.setName("");
                transaction.add(op.update(logicalSwitch)
                        .where(extraLogicalSwitch.getNameColumn().getSchema().opEqual(existingLogicalSwitchName))
                        .build());
            }
        }
    }

    private void setDescription(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch) {
        if(inputSwitch.getHwvtepNodeDescription() != null) {
            logicalSwitch.setDescription(inputSwitch.getHwvtepNodeDescription());
        }
    }

    private void setName(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch,
            Optional<LogicalSwitches> inputSwitchOptional) {
        if (inputSwitch.getHwvtepNodeName() != null) {
            logicalSwitch.setName(inputSwitch.getHwvtepNodeName().getValue());
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.get().getHwvtepNodeName() != null) {
            logicalSwitch.setName(inputSwitchOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setTunnelKey(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch) {
        if (inputSwitch.getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<Long>();
            tunnel.add(Long.valueOf(inputSwitch.getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        }
    }

    private Map<InstanceIdentifier<Node>, List<LogicalSwitches>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<LogicalSwitches> class1) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> result
            = new HashMap<InstanceIdentifier<Node>, List<LogicalSwitches>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<LogicalSwitches> lswitchListUpdated = created.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    if (lswitchListUpdated != null) {
                        result.put(key, lswitchListUpdated);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, List<LogicalSwitches>> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<LogicalSwitches> class1) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> result
            = new HashMap<InstanceIdentifier<Node>, List<LogicalSwitches>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<LogicalSwitches> lswitchListUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    List<LogicalSwitches> lswitchListBefore = before.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    if (lswitchListUpdated != null) {
                        if (lswitchListBefore != null) {
                            lswitchListUpdated.removeAll(lswitchListBefore);
                        }
                        result.put(key, lswitchListUpdated);
                    }
                }
            }
        }
        return result;
    }
}
