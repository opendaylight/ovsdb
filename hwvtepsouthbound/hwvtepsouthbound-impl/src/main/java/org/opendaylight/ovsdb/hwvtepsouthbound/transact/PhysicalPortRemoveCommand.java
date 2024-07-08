/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalPortRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortRemoveCommand.class);

    public PhysicalPortRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        //TODO reuse from base class instead of extractRemovedPorts
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> removeds =
                extractRemovedPorts(getChanges(), HwvtepPhysicalPortAugmentation.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> removed:
                removeds.entrySet()) {
                updatePhysicalPort(transaction, removed.getKey(), removed.getValue());
            }
        }
    }

    private void updatePhysicalPort(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> psNodeiid,
                                    final List<HwvtepPhysicalPortAugmentation> listPort) {
        final var op = ops();

        for (HwvtepPhysicalPortAugmentation port : listPort) {
            LOG.debug("Updating a physical port named: {}", port.getHwvtepNodeName().getValue());
            Optional<HwvtepPhysicalPortAugmentation> operationalPhysicalPortOptional =
                    getOperationalState().getPhysicalPortAugmentation(psNodeiid, port.getHwvtepNodeName());
            if (operationalPhysicalPortOptional.isPresent()) {
                PhysicalPort physicalPort = transaction.getTypedRowWrapper(PhysicalPort.class);
                physicalPort.setVlanBindings(new HashMap<>());
                HwvtepPhysicalPortAugmentation updatedPhysicalPort = operationalPhysicalPortOptional.orElseThrow();
                String existingPhysicalPortName = updatedPhysicalPort.getHwvtepNodeName().getValue();
                PhysicalPort extraPhyscialPort = transaction.getTypedRowWrapper(PhysicalPort.class);
                extraPhyscialPort.setName("");
                LOG.trace("execute: updating physical port: {}", physicalPort);
                transaction.add(op.update(physicalPort)
                        .where(extraPhyscialPort.getNameColumn().getSchema().opEqual(existingPhysicalPortName))
                        .build());
                updateControllerTxHistory(TransactionType.UPDATE, physicalPort);
            } else {
                LOG.warn("Unable to update physical port {} because it was not found in the operational store, "
                        + "and thus we cannot retrieve its UUID", port.getHwvtepNodeName().getValue());
            }
        }
    }

    protected Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> extractRemovedPorts(
            final Collection<DataTreeModification<Node>> changes, final Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                //If the node which physical ports belong to is removed, all physical ports
                //should be removed too.
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    List<HwvtepPhysicalPortAugmentation> lswitchListRemoved = new ArrayList<>();
                    for (TerminationPoint tp : removed.nonnullTerminationPoint().values()) {
                        HwvtepPhysicalPortAugmentation hppAugmentation =
                                tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                        if (hppAugmentation != null) {
                            lswitchListRemoved.add(hppAugmentation);
                        }
                    }
                    if (!lswitchListRemoved.isEmpty()) {
                        result.put(key, lswitchListRemoved);
                    }
                }
                //If the node which physical ports belong to is updated, and physical ports may
                //be created or updated or deleted, we need to get deleted ones.
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<HwvtepPhysicalPortAugmentation> portListUpdated = new ArrayList<>();
                    for (TerminationPoint tp : updated.nonnullTerminationPoint().values()) {
                        HwvtepPhysicalPortAugmentation hppAugmentation =
                                tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                        if (hppAugmentation != null) {
                            portListUpdated.add(hppAugmentation);
                        }
                    }
                    List<HwvtepPhysicalPortAugmentation> portListBefore = new ArrayList<>();
                    for (TerminationPoint tp : before.nonnullTerminationPoint().values()) {
                        HwvtepPhysicalPortAugmentation hppAugmentation =
                                tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                        if (hppAugmentation != null) {
                            portListBefore.add(hppAugmentation);
                        }
                    }
                    portListBefore.removeAll(portListUpdated);
                    //then exclude updated physical ports
                    List<HwvtepPhysicalPortAugmentation> portListRemoved = new ArrayList<>();
                    for (HwvtepPhysicalPortAugmentation portBefore: portListBefore) {
                        int index = 0;
                        for (; index < portListUpdated.size(); index++) {
                            if (portBefore.getHwvtepNodeName().equals(portListUpdated.get(index).getHwvtepNodeName())) {
                                break;
                            }
                        }
                        if (index == portListUpdated.size()) {
                            portListRemoved.add(portBefore);
                        }
                    }
                    if (!portListRemoved.isEmpty()) {
                        result.put(key, portListRemoved);
                    }
                }
            }
        }
        return result;
    }
}
