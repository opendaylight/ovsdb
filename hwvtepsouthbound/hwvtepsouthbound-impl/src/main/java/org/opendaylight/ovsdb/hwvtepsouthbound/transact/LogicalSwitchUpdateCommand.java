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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
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
        Map<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> created =
                extractCreated(getChanges(),LogicalSwitches.class);
        if (!created.isEmpty()) {
            for(Entry<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> logicalSwitchEntry:
                created.entrySet()) {
                updateLogicalSwitch(transaction,  logicalSwitchEntry.getKey(), logicalSwitchEntry.getValue());
            }
        }
        Map<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> updated =
                extractUpdated(getChanges(),LogicalSwitches.class);
        if(!updated.isEmpty()) {
            for(Entry<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> logicalSwitchEntry:
                updated.entrySet()) {
                updateLogicalSwitch(transaction,  logicalSwitchEntry.getKey(), logicalSwitchEntry.getValue());
            }
        }
    }


    private void updateLogicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<LogicalSwitches> iid, LogicalSwitches logicalSwitches) {
        LOG.debug("Creating a logical switch named: {}", logicalSwitches.getHwvtepNodeName());
        Optional<LogicalSwitches> operationalLogicalSwitchOptional =
                getOperationalState().getLogicalSwitches(iid);
        DatabaseSchema dbSchema = transaction.getDatabaseSchema();
        LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
        if(!operationalLogicalSwitchOptional.isPresent()) {
            setName(logicalSwitch, logicalSwitches, operationalLogicalSwitchOptional);
            setTunnelKey(logicalSwitch, logicalSwitches, operationalLogicalSwitchOptional);
            transaction.add(op.insert(logicalSwitch));
        } else {
            String existingLogicalSwitchName = operationalLogicalSwitchOptional.get().getHwvtepNodeName().getValue();
            // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
            LogicalSwitch extraLogicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
            extraLogicalSwitch.setName("");
            transaction.add(op.update(logicalSwitch)
                    .where(extraLogicalSwitch.getNameColumn().getSchema().opEqual(existingLogicalSwitchName))
                    .build());
            //stampInstanceIdentifier(transaction, iid.firstIdentifierOf(Node.class),existingBridgeName);
        }
    }

    private void setName(LogicalSwitch logicalSwitch, LogicalSwitches logicalSwitches,
            Optional<LogicalSwitches> operationalLogicalSwitchOptional) {
        if(logicalSwitches.getHwvtepNodeName() != null) {
            logicalSwitch.setName(logicalSwitches.getHwvtepNodeName().getValue());
        } else if(operationalLogicalSwitchOptional.isPresent() && operationalLogicalSwitchOptional.get().getHwvtepNodeName() != null) {
            logicalSwitch.setName(operationalLogicalSwitchOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setTunnelKey(LogicalSwitch logicalSwitch, LogicalSwitches logicalSwitches,
            Optional<LogicalSwitches> operationalLogicalSwitchOptional) {
        if(logicalSwitches.getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<Long>();
            tunnel.add(Long.valueOf(logicalSwitches.getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        } else if(operationalLogicalSwitchOptional.isPresent() && operationalLogicalSwitchOptional.get().getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<Long>();
            tunnel.add(Long.valueOf(operationalLogicalSwitchOptional.get().getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        }
    }

    private Map<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<LogicalSwitches> class1) {
        Map<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> result
            = new HashMap<InstanceIdentifier<LogicalSwitches>, LogicalSwitches>();
        if(changes != null && !changes.isEmpty()) {
            for(DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if(created != null) {
/*                    LogicalSwitches logicalSwitch = created.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    InstanceIdentifier<LogicalSwitches> iid = change.getRootPath().getRootIdentifier().augmentation(LogicalSwitches.class);
                    if(logicalSwitch != null) {
                        result.put(iid, logicalSwitch);
                    }*/
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<LogicalSwitches> class1) {
        Map<InstanceIdentifier<LogicalSwitches>, LogicalSwitches> result
            = new HashMap<InstanceIdentifier<LogicalSwitches>, LogicalSwitches>();
        if(changes != null && !changes.isEmpty()) {
            for(DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                if(updated != null) {
/*                    LogicalSwitches logicalSwitch = updated.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
                    InstanceIdentifier<LogicalSwitches> iid = change.getRootPath().getRootIdentifier().augmentation(LogicalSwitches.class);
                    if(logicalSwitch != null) {
                        result.put(iid, logicalSwitch);
                    }*/
                }
            }
        }
        return result;
    }
}
