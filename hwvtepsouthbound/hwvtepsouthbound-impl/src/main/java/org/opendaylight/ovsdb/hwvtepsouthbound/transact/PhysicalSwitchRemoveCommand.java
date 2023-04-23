/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalSwitchRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalSwitchRemoveCommand.class);

    public PhysicalSwitchRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> removeds =
                extractRemovedSwitches(getChanges(),PhysicalSwitchAugmentation.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> removed:
                removeds.entrySet()) {
                removePhysicalSwitch(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }

    private void removePhysicalSwitch(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> iid, final PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        LOG.debug("Removing a physical switch named: {}", physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional =
                getOperationalState().getPhysicalSwitchAugmentation(iid);
        PhysicalSwitch physicalSwitch = transaction.getTypedRowSchema(PhysicalSwitch.class);
        if (operationalPhysicalSwitchOptional.isPresent()
                && operationalPhysicalSwitchOptional.orElseThrow().getPhysicalSwitchUuid() != null) {
            UUID physicalSwitchUuid = new UUID(operationalPhysicalSwitchOptional.orElseThrow()
                    .getPhysicalSwitchUuid().getValue());
            Global global = transaction.getTypedRowSchema(Global.class);
            transaction.add(op.delete(physicalSwitch.getSchema())
                    .where(physicalSwitch.getUuidColumn().getSchema().opEqual(physicalSwitchUuid)).build());
            transaction.add(op.comment("Physical Switch: Deleting "
                    + physicalSwitchAugmentation.getHwvtepNodeName().getValue()));
            transaction.add(op.mutate(global.getSchema())
                    .addMutation(global.getSwitchesColumn().getSchema(), Mutator.DELETE,
                            Collections.singleton(physicalSwitchUuid)));
            transaction.add(op.comment("Global: Mutating " + physicalSwitchAugmentation.getHwvtepNodeName().getValue()
                    + " " + physicalSwitchUuid));
        } else {
            LOG.warn("Unable to delete physical switch {} because it was not found in the operational store",
                    physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        }
    }

    private static Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractRemovedSwitches(
            final Collection<DataTreeModification<Node>> changes, final Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    PhysicalSwitchAugmentation physicalSwitch =
                            removed.augmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }
}
