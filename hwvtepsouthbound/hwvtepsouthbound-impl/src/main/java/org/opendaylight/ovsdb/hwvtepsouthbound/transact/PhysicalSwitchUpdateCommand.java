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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class PhysicalSwitchUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalSwitchUpdateCommand.class);

    public PhysicalSwitchUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> created =
                extractCreated(getChanges(),PhysicalSwitchAugmentation.class);
        if (!created.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> physicalSwitchEntry:
                created.entrySet()) {
                updatePhysicalSwitch(transaction,  physicalSwitchEntry.getKey(), physicalSwitchEntry.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> updated =
                extractUpdated(getChanges(),PhysicalSwitchAugmentation.class);
        if (!updated.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> physicalSwitchEntry:
                updated.entrySet()) {
                updatePhysicalSwitch(transaction,  physicalSwitchEntry.getKey(), physicalSwitchEntry.getValue());
            }
        }
    }


    private void updatePhysicalSwitch(TransactionBuilder transaction,
            InstanceIdentifier<Node> iid, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        LOG.debug("Creating a physical switch named: {}", physicalSwitchAugmentation.getHwvtepNodeName());
        Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional =
                getOperationalState().getPhysicalSwitchAugmentation(iid);
        PhysicalSwitch physicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalSwitch.class);
        setDescription(physicalSwitch, physicalSwitchAugmentation);
        setManagementIps(physicalSwitch, physicalSwitchAugmentation);
        setTunnuleIps(physicalSwitch, physicalSwitchAugmentation);
        setTunnels(physicalSwitch, physicalSwitchAugmentation);
        if (!operationalPhysicalSwitchOptional.isPresent()) {
            //create a physical switch
            setName(physicalSwitch, physicalSwitchAugmentation, operationalPhysicalSwitchOptional);
            String pswitchUuid = "PhysicalSwitch_" + HwvtepSouthboundMapper.getRandomUUID();
            transaction.add(op.insert(physicalSwitch).withId(pswitchUuid));
            //update global table
            Global global = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Global.class);
            global.setSwitches(Sets.newHashSet(new UUID(pswitchUuid)));

            LOG.info("execute: physical switch: {}", physicalSwitch);
            transaction.add(op.mutate(global)
                    .addMutation(global.getSwitchesColumn().getSchema(), Mutator.INSERT,
                            global.getSwitchesColumn().getData()));
        } else {
            PhysicalSwitchAugmentation updatedPhysicalSwitch = operationalPhysicalSwitchOptional.get();
            String existingPhysicalSwitchName = updatedPhysicalSwitch.getHwvtepNodeName().getValue();
            // Name is immutable, and so we *can't* update it.  So we use extraPhysicalSwitch for the schema stuff
            PhysicalSwitch extraPhysicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalSwitch.class);
            extraPhysicalSwitch.setName("");
            transaction.add(op.update(physicalSwitch)
                    .where(extraPhysicalSwitch.getNameColumn().getSchema().opEqual(existingPhysicalSwitchName))
                    .build());
        }
    }

    private void setName(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation,
            Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional) {
        if (physicalSwitchAugmentation.getHwvtepNodeName() != null) {
            physicalSwitch.setName(physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        } else if (operationalPhysicalSwitchOptional.isPresent() && operationalPhysicalSwitchOptional.get().getHwvtepNodeName() != null) {
            physicalSwitch.setName(operationalPhysicalSwitchOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setDescription(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        if (physicalSwitchAugmentation.getHwvtepNodeDescription() != null) {
            physicalSwitch.setDescription(physicalSwitchAugmentation.getHwvtepNodeDescription());
        }
    }

    private void setManagementIps(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        Set<String> ipSet = new HashSet<String>();
        if (physicalSwitchAugmentation.getManagementIps() != null) {
            for (ManagementIps ip: physicalSwitchAugmentation.getManagementIps()) {
                ipSet.add(ip.getManagementIpsKey().getIpv4Address().getValue());
            }
            physicalSwitch.setManagementIps(ipSet);
        }
    }

    private void setTunnuleIps(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        Set<String> ipSet = new HashSet<String>();
        if (physicalSwitchAugmentation.getTunnelIps() != null) {
            for (TunnelIps ip: physicalSwitchAugmentation.getTunnelIps()) {
                ipSet.add(ip.getTunnelIpsKey().getIpv4Address().getValue());
            }
            physicalSwitch.setTunnelIps(ipSet);
        }
    }

    private void setTunnels(PhysicalSwitch physicalSwitch, PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        //TODO
    }

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractCreated(
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

    private Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result
            = new HashMap<InstanceIdentifier<Node>, PhysicalSwitchAugmentation>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                if (updated != null) {
                    PhysicalSwitchAugmentation physicalSwitch = updated.getAugmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }
}