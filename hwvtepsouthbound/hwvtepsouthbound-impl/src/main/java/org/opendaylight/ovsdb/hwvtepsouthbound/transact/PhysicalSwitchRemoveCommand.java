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
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class PhysicalSwitchRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalSwitchRemoveCommand.class);

    public PhysicalSwitchRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> removeds =
                extractRemoved(getChanges(),PhysicalSwitchAugmentation.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> removed:
                removeds.entrySet()) {
                removePhysicalSwitch(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }

    private void removePhysicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<Node> iid, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        LOG.debug("Removing a physical switch named: {}", physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional =
                getOperationalState().getPhysicalSwitchAugmentation(iid);
        PhysicalSwitch physicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalSwitch.class, null);
        if (operationalPhysicalSwitchOptional.isPresent()) {
            transaction.add(op.delete(physicalSwitch.getSchema())
                    .where(physicalSwitch.getNameColumn().getSchema().opEqual(physicalSwitchAugmentation.getHwvtepNodeName().getValue())).build());
            transaction.add(op.comment("Physical Switch: Deleting " + physicalSwitchAugmentation.getHwvtepNodeName().getValue()));
        } else {
            LOG.warn("Unable to delete physical switch {} because it was not found in the operational store",
                    physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        }
    }

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, PhysicalSwitchAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    PhysicalSwitchAugmentation physicalSwitch = removed.getAugmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }
}
