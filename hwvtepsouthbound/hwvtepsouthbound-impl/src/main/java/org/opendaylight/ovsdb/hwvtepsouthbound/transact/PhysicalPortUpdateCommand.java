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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class PhysicalPortUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortUpdateCommand.class);

    public PhysicalPortUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> createds =
                extractCreated(getChanges(),HwvtepPhysicalPortAugmentation.class);
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> createdPhysicalSwitches =
                extractCreatedPhyscialSwitch(getChanges(),PhysicalSwitchAugmentation.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> created:
                createds.entrySet()) {
                updatePhysicalPort(transaction,  created.getKey(), created.getValue(), createdPhysicalSwitches);
            }
        }
        Map<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> updateds =
                extractUpdated(getChanges(),HwvtepPhysicalPortAugmentation.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> updated:
                updateds.entrySet()) {
                updatePhysicalPort(transaction,  updated.getKey(), updated.getValue(), createdPhysicalSwitches);
            }
        }
    }

    private void updatePhysicalPort(TransactionBuilder transaction,
            InstanceIdentifier<Node> iid,
            HwvtepPhysicalPortAugmentation physicalPortAugmentation,
            Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> createdPhysicalSwitches ) {
        LOG.debug("Creating a physical port named: {}", physicalPortAugmentation.getHwvtepNodeName().toString());
        Optional<HwvtepPhysicalPortAugmentation> operationalPhysicalPortOptional =
                getOperationalState().getPhysicalPortAugmentation(iid);
        PhysicalPort physicalPort = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalPort.class);
        setDescription(physicalPort, physicalPortAugmentation);
        setVlanBindings(physicalPort, physicalPortAugmentation);
        if (!operationalPhysicalPortOptional.isPresent()) {
            Optional<PhysicalSwitchAugmentation> physicalSwitchOptional =
                    getOperationalState().getPhysicalSwitchAugmentation(iid);
            PhysicalSwitchAugmentation physicalSwitchAugmentation = null;
            if (physicalSwitchOptional.isPresent()) {
                physicalSwitchAugmentation = physicalSwitchOptional.get();
            } else {
                physicalSwitchAugmentation = createdPhysicalSwitches.get(iid);
            }

            setName(physicalPort, physicalPortAugmentation, operationalPhysicalPortOptional);
            String portNamedUuidString = HwvtepSouthboundMapper.getRandomUUID();
            UUID portNamedUuid = new UUID(portNamedUuidString);
            transaction.add(op.insert(physicalPort).withId(portNamedUuidString));

            PhysicalSwitch physicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalSwitch.class);
            physicalSwitch.setName(physicalSwitchAugmentation.getHwvtepNodeName().toString());
            physicalSwitch.setPorts(Sets.newHashSet(portNamedUuid));
            LOG.info("execute: physical switch: {}", physicalSwitch);
            transaction.add(op.mutate(physicalSwitch)
                    .addMutation(physicalSwitch.getPortsColumn().getSchema(), Mutator.INSERT,
                            physicalSwitch.getPortsColumn().getData())
                    .where(physicalSwitch.getNameColumn().getSchema().opEqual(physicalSwitch.getNameColumn().getData()))
                    .build());
        } else {
            HwvtepPhysicalPortAugmentation updatedPhysicalPort = operationalPhysicalPortOptional.get();
            String existingPhysicalPortName = updatedPhysicalPort.getHwvtepNodeName().getValue();
            PhysicalPort extraPhyscialPort =
                    TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalPort.class);
            extraPhyscialPort.setName("");
            transaction.add(op.update(physicalPort)
                    .where(extraPhyscialPort.getNameColumn().getSchema().opEqual(existingPhysicalPortName))
                    .build());
        }
    }

    private void setName(PhysicalPort physicalPort, HwvtepPhysicalPortAugmentation inputPhysicalPort,
            Optional<HwvtepPhysicalPortAugmentation> operationalLogicalSwitchOptional) {
        if (inputPhysicalPort.getHwvtepNodeName() != null) {
            physicalPort.setName(inputPhysicalPort.getHwvtepNodeName().toString());
        } else if (operationalLogicalSwitchOptional.isPresent()
                && operationalLogicalSwitchOptional.get().getHwvtepNodeName() != null) {
            physicalPort.setName(operationalLogicalSwitchOptional.get().getHwvtepNodeName().toString());
        }
    }

    private void setDescription(PhysicalPort physicalPort, HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getHwvtepNodeDescription() != null) {
            physicalPort.setDescription(inputPhysicalPort.getHwvtepNodeDescription().toString());
        }
    }

    private void setVlanBindings(PhysicalPort physicalPort, HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getVlanBindings() != null) {
            //TODO: How to get UUID from logical switch name?
            /*Map<Long, UUID> bindingMap = new HashMap<Long, UUID>();
            for (VlanBindings vlanBinding: inputPhysicalPort.getVlanBindings()) {
                bindingMap.put(vlanBinding.getVlanIdKey().getValue(), vlanBinding.getLogicalSwitchRef().getValue());
            }
            physicalPort.setVlanBindings(bindingMap);*/
        }
    }

    private Map<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<TerminationPoint> physicalPorts = created.getTerminationPoint();
                    if (physicalPorts != null) {
                        for (TerminationPoint physicalPort : physicalPorts) {
                            HwvtepPhysicalPortAugmentation physicalPortAugmentation =
                                    physicalPort.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (physicalPortAugmentation != null) {
                                result.put(key, physicalPortAugmentation);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractCreatedPhyscialSwitch(
            Collection<DataTreeModification<Node>> changes, Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, PhysicalSwitchAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    PhysicalSwitchAugmentation physicalSwitch = created.getAugmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, HwvtepPhysicalPortAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                if (updated != null) {
                    List<TerminationPoint> physicalPorts = updated.getTerminationPoint();
                    if (physicalPorts != null) {
                        for (TerminationPoint physicalPort : physicalPorts) {
                            HwvtepPhysicalPortAugmentation physicalPortAugmentation =
                                    physicalPort.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (physicalPortAugmentation != null) {
                                result.put(key, physicalPortAugmentation);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}