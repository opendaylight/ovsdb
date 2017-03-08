/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
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
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> createds =
                extractCreated(getChanges(),HwvtepPhysicalPortAugmentation.class);
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> createdPhysicalSwitches =
                extractCreatedPhyscialSwitch(getChanges(),PhysicalSwitchAugmentation.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> created:
                createds.entrySet()) {
                updatePhysicalPort(transaction,  created.getKey(), created.getValue(), createdPhysicalSwitches);
            }
        }
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> updateds =
                extractUpdatedPorts(getChanges(), HwvtepPhysicalPortAugmentation.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> updated:
                updateds.entrySet()) {
                updatePhysicalPort(transaction,  updated.getKey(), updated.getValue(), createdPhysicalSwitches);
            }
        }
    }

    private void updatePhysicalPort(TransactionBuilder transaction,
            InstanceIdentifier<Node> psNodeiid,
            List<HwvtepPhysicalPortAugmentation> listPort,
            Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> createdPhysicalSwitches ) {
        //Get physical switch which the port belong to: in operation DS or new created
        PhysicalSwitchAugmentation physicalSwitchBelong = getPhysicalSwitchBelong(psNodeiid, createdPhysicalSwitches);
        for (HwvtepPhysicalPortAugmentation port : listPort) {
            LOG.debug("Creating a physical port named: {}", port.getHwvtepNodeName().getValue());
            Optional<HwvtepPhysicalPortAugmentation> operationalPhysicalPortOptional =
                    getOperationalState().getPhysicalPortAugmentation(psNodeiid, port.getHwvtepNodeName());
            PhysicalPort physicalPort = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalPort.class);
            //get managing global node of physicalSwitchBelong
            InstanceIdentifier<?> globalNodeIid = physicalSwitchBelong.getManagedBy().getValue();
            setVlanBindings(globalNodeIid, physicalPort, port);
            setDescription(physicalPort, port);
            if (!operationalPhysicalPortOptional.isPresent()) {
                //create a physical port
                setName(physicalPort, port, operationalPhysicalPortOptional);
                String portUuid = "PhysicalPort_" + HwvtepSouthboundMapper.getRandomUUID();
                LOG.trace("execute: creating physical port: {}", physicalPort);
                transaction.add(op.insert(physicalPort).withId(portUuid));
                transaction.add(op.comment("Physical Port: Creating " + port.getHwvtepNodeName().getValue()));
                //update physical switch table
                PhysicalSwitch physicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalSwitch.class);
                physicalSwitch.setName(physicalSwitchBelong.getHwvtepNodeName().getValue());
                physicalSwitch.setPorts(Sets.newHashSet(new UUID(portUuid)));
                LOG.trace("execute: mutating physical switch: {}", physicalSwitch);
                transaction.add(op.mutate(physicalSwitch)
                        .addMutation(physicalSwitch.getPortsColumn().getSchema(), Mutator.INSERT,
                                physicalSwitch.getPortsColumn().getData())
                        .where(physicalSwitch.getNameColumn().getSchema().opEqual(physicalSwitch.getNameColumn().getData()))
                        .build());
                transaction.add(op.comment("Physical Switch: Mutating " +
                                port.getHwvtepNodeName().getValue() + " " + portUuid));
            } else {
                //updated physical port only
                HwvtepPhysicalPortAugmentation updatedPhysicalPort = operationalPhysicalPortOptional.get();
                String existingPhysicalPortName = updatedPhysicalPort.getHwvtepNodeName().getValue();
                PhysicalPort extraPhyscialPort =
                        TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalPort.class);
                extraPhyscialPort.setName("");
                LOG.trace("execute: updating physical port: {}", physicalPort);
                transaction.add(op.update(physicalPort)
                        .where(extraPhyscialPort.getNameColumn().getSchema().opEqual(existingPhysicalPortName))
                        .build());
                transaction.add(op.comment("Physical Port: Updating " + existingPhysicalPortName));
            }
        }
    }

    private PhysicalSwitchAugmentation getPhysicalSwitchBelong(InstanceIdentifier<Node> psNodeiid,
            Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> createdPhysicalSwitches) {
        Optional<PhysicalSwitchAugmentation> physicalSwitchOptional =
                    getOperationalState().getPhysicalSwitchAugmentation(psNodeiid);
        PhysicalSwitchAugmentation physicalSwitchAugmentation = null;
        if (physicalSwitchOptional.isPresent()) {
            physicalSwitchAugmentation = physicalSwitchOptional.get();
        } else {
            physicalSwitchAugmentation = createdPhysicalSwitches.get(psNodeiid);
        }
        return physicalSwitchAugmentation;
    }

    private void setName(PhysicalPort physicalPort, HwvtepPhysicalPortAugmentation inputPhysicalPort,
            Optional<HwvtepPhysicalPortAugmentation> operationalLogicalSwitchOptional) {
        if (inputPhysicalPort.getHwvtepNodeName() != null) {
            physicalPort.setName(inputPhysicalPort.getHwvtepNodeName().getValue());
        } else if (operationalLogicalSwitchOptional.isPresent()
                && operationalLogicalSwitchOptional.get().getHwvtepNodeName() != null) {
            physicalPort.setName(operationalLogicalSwitchOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setDescription(PhysicalPort physicalPort, HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getHwvtepNodeDescription() != null) {
            physicalPort.setDescription(inputPhysicalPort.getHwvtepNodeDescription().toString());
        }
    }

    private void setVlanBindings(InstanceIdentifier<?> globalNodeIid, PhysicalPort physicalPort,
            HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getVlanBindings() != null) {
            //get UUID by LogicalSwitchRef
            Map<Long, UUID> bindingMap = new HashMap<>();
            for (VlanBindings vlanBinding: inputPhysicalPort.getVlanBindings()) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<LogicalSwitches> lswitchIid =
                        (InstanceIdentifier<LogicalSwitches>) vlanBinding.getLogicalSwitchRef().getValue();
                Optional<LogicalSwitches> operationalSwitchOptional =
                        getOperationalState().getLogicalSwitches(lswitchIid);
                if (operationalSwitchOptional.isPresent()) {
                    Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                    bindingMap.put(vlanBinding.getVlanIdKey().getValue().longValue(), new UUID(logicalSwitchUuid.getValue()));
                }else{
                    bindingMap.put(vlanBinding.getVlanIdKey().getValue().longValue(), TransactUtils.getLogicalSwitchUUID(lswitchIid));
                }
            }
            physicalPort.setVlanBindings(bindingMap);
        }
    }

    private Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<HwvtepPhysicalPortAugmentation> portListUpdated = new ArrayList<>();
                    if (created.getTerminationPoint() != null) {
                        for (TerminationPoint tp : created.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListUpdated.add(hppAugmentation);
                            }
                        }
                    }
                    result.put(key, portListUpdated);
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractCreatedPhyscialSwitch(
            Collection<DataTreeModification<Node>> changes, Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result = new HashMap<>();
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

    private Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> extractUpdatedPorts(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<HwvtepPhysicalPortAugmentation> portListUpdated = new ArrayList<>();
                    List<HwvtepPhysicalPortAugmentation> portListBefore = new ArrayList<>();
                    if (updated.getTerminationPoint() != null) {
                        for (TerminationPoint tp : updated.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListUpdated.add(hppAugmentation);
                            }
                        }
                    }
                    if (before.getTerminationPoint() != null) {
                        for (TerminationPoint tp : before.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListBefore.add(hppAugmentation);
                            }
                        }
                    }
                    portListUpdated.removeAll(portListBefore);
                    result.put(key, portListUpdated);
                }
            }
        }
        return result;
    }
}