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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchAugmentation;
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
        //TODO
        Map<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>, HwvtepLogicalSwitchAugmentation> created =
                extractCreated(getChanges(),HwvtepLogicalSwitchAugmentation.class);
        if(created != null) {
            for(Entry<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>, HwvtepLogicalSwitchAugmentation> logicalSwitchEntry:
                created.entrySet()) {
                LOG.info("1111111 newcreated entry {}",logicalSwitchEntry.getValue().getClass());
                updateLogicalSwitch(transaction,  logicalSwitchEntry.getKey(), logicalSwitchEntry.getValue());
            }
        }
    }


    private void updateLogicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<HwvtepLogicalSwitchAugmentation> iid, HwvtepLogicalSwitchAugmentation logicalSwitchAugmentation) {
        // TODO Auto-generated method stub
        LOG.info("Creating a logical switch named: {}", logicalSwitchAugmentation.getHwvtepNodeName());
        Optional<HwvtepLogicalSwitchAugmentation> operationalLogicalSwitchOptional =
                getOperationalState().getLogicalSwitchAugmentation(iid);
        DatabaseSchema dbSchema = transaction.getDatabaseSchema();
        LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
        if(!operationalLogicalSwitchOptional.isPresent()) {
            setName(logicalSwitch, logicalSwitchAugmentation, operationalLogicalSwitchOptional);
            setTunnelKey(logicalSwitch, logicalSwitchAugmentation, operationalLogicalSwitchOptional);
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

    private void setName(LogicalSwitch logicalSwitch, HwvtepLogicalSwitchAugmentation logicalSwitchAugmentation,
            Optional<HwvtepLogicalSwitchAugmentation> operationalLogicalSwitchOptional) {
        if(logicalSwitchAugmentation.getHwvtepNodeName() != null) {
            logicalSwitch.setName(logicalSwitchAugmentation.getHwvtepNodeName().getValue());
        } else if(operationalLogicalSwitchOptional.isPresent() && operationalLogicalSwitchOptional.get().getHwvtepNodeName() != null) {
            logicalSwitch.setName(operationalLogicalSwitchOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setTunnelKey(LogicalSwitch logicalSwitch, HwvtepLogicalSwitchAugmentation logicalSwitchAugmentation,
            Optional<HwvtepLogicalSwitchAugmentation> operationalLogicalSwitchOptional) {
        if(logicalSwitchAugmentation.getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<Long>();
            tunnel.add(Long.valueOf(logicalSwitchAugmentation.getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        } else if(operationalLogicalSwitchOptional.isPresent() && operationalLogicalSwitchOptional.get().getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<Long>();
            tunnel.add(Long.valueOf(operationalLogicalSwitchOptional.get().getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        }
    }

    private Node getCreated(DataObjectModification<Node> mod) {
        if((mod.getModificationType() == ModificationType.WRITE)
                        && (mod.getDataBefore() == null)){
            return mod.getDataAfter();
        }
        return null;
    }

    private Map<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>, HwvtepLogicalSwitchAugmentation> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepLogicalSwitchAugmentation> class1) {
        // TODO Auto-generated method stub
        Map<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>, HwvtepLogicalSwitchAugmentation> result 
            = new HashMap<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>, HwvtepLogicalSwitchAugmentation>();
        if(changes != null && !changes.isEmpty()) {
            for(DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = getCreated(mod);
                if(created != null) {
                    HwvtepLogicalSwitchAugmentation logicalSwitch = created.getAugmentation(HwvtepLogicalSwitchAugmentation.class);
                    InstanceIdentifier<HwvtepLogicalSwitchAugmentation> iid = change.getRootPath().getRootIdentifier().augmentation(HwvtepLogicalSwitchAugmentation.class);
                    if(logicalSwitch != null) {
                        result.put(iid, logicalSwitch);
                    }
                }
            }
        }
        return result;
    }
}
