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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchAugmentation;
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
        //TODO
		/*Set<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>> removeds =
                TransactUtils.extractRemoved(getChanges(),HwvtepLogicalSwitchAugmentation.class);
        Map<InstanceIdentifier<HwvtepLogicalSwitchAugmentation>, HwvtepLogicalSwitchAugmentation> originals
            = TransactUtils.extractOriginal(getChanges(),HwvtepLogicalSwitchAugmentation.class);
        for (InstanceIdentifier<HwvtepLogicalSwitchAugmentation> removed: removeds) {
            LOG.info("Received request to delete ovsdb node {}",removed);
            HwvtepLogicalSwitchAugmentation original = originals.get(removed);
            LogicalSwitch bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class, null);
            Optional<HwvtepLogicalSwitchAugmentation> lsAugmentationOptional = getOperationalState()
                    .getLogicalSwitchAugmentation(removed);
            if (lsAugmentationOptional.isPresent() && lsAugmentationOptional.get().getHwvtepLogicalSwitchExternalId() != null) {
                UUID lsUuid = new UUID(lsAugmentationOptional.get().getHwvtepLogicalSwitchExternalId().getValue());
                Global ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                        Global.class,null);
                transaction.add(op.delete(bridge.getSchema())
                        .where(bridge.getUuidColumn().getSchema().opEqual(lsUuid)).build());
                transaction.add(op.comment("Logical Switch: Deleting " + original.getHwvtepNodeName()));
            } else {
                LOG.warn("Unable to delete logical switch {} because it was not found in the operational store, "
                        + "and thus we cannot retrieve its UUID", removed);
            }

        }*/
    }
}
