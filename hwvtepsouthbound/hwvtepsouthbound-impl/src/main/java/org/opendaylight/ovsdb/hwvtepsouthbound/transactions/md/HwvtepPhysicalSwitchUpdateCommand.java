/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.SwitchFaultStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.SwitchFaultStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.SwitchFaultStatusKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepPhysicalSwitchUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepPhysicalSwitchUpdateCommand.class);
    private Map<UUID, PhysicalSwitch> updatedPSRows;
    private Map<UUID, Tunnel> updatedTunnelRows;
    private Map<UUID, PhysicalSwitch> oldPSRows;

    public HwvtepPhysicalSwitchUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPSRows = TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(), getDbSchema());
        oldPSRows = TyperUtils.extractRowsOld(PhysicalSwitch.class, getUpdates(), getDbSchema());
        try {
            updatedTunnelRows = TyperUtils.extractRowsUpdated(Tunnel.class, getUpdates(), getDbSchema());
        } catch (IllegalArgumentException e) {
            LOG.debug("Tunnel Table not supported on this HWVTEP device", e.getMessage());
        }
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Map.Entry<UUID, PhysicalSwitch> entry : updatedPSRows.entrySet()) {
            updatePhysicalSwitch(transaction, entry.getKey(), entry.getValue());
        }
    }

    private void updatePhysicalSwitch(ReadWriteTransaction transaction, UUID uuid, PhysicalSwitch pSwitch) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present", connection);
            // Update the connection node to let it know it manages this
            // Physical Switch
            Node connectionNode = buildConnectionNode(pSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);

            // Update the Physical Switch with whatever data we are getting
            InstanceIdentifier<Node> psIid = getInstanceIdentifier(pSwitch);
            Node psNode = buildPhysicalSwitchNode(connection.get(), pSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, psIid, psNode);

            PhysicalSwitch oldPSwitch = oldPSRows.get(uuid);
            updateTunnelIps(pSwitch, oldPSwitch, transaction);

            getOvsdbConnectionInstance().getDeviceInfo().putPhysicalSwitch(pSwitch.getUuid(), pSwitch);
            // TODO: Delete entries that are no longer needed
            // TODO: Deletion of tunnels
            // TODO: Deletion of Tunnel BFD config and params
            //Deleting old switch fault status entries
            deleteEntries(transaction, getSwitchFaultStatusToRemove(psIid,pSwitch));
        }
    }

    private InstanceIdentifier<TunnelIps> getTunnelIpIid(final String tunnelIp, final InstanceIdentifier<Node> psIid) {
        IpAddress ip = new IpAddress(tunnelIp.toCharArray());
        TunnelIps tunnelIps = new TunnelIpsBuilder().setKey(new TunnelIpsKey(ip)).setTunnelIpsKey(ip).build();
        return psIid.augmentation(PhysicalSwitchAugmentation.class).child(TunnelIps.class, tunnelIps.getKey());
    }

    private void updateTunnelIps(final PhysicalSwitch newPSwitch, final PhysicalSwitch oldPSwitch,
                                 final ReadWriteTransaction transaction) {
        Set<String> oldTunnelIps = oldPSwitch != null && oldPSwitch.getTunnelIpsColumn() != null ? oldPSwitch
                .getTunnelIpsColumn().getData() : Collections.EMPTY_SET;
        Set<String> newTunelIps = newPSwitch != null && newPSwitch.getTunnelIpsColumn() != null ? newPSwitch
                .getTunnelIpsColumn().getData() : Collections.EMPTY_SET;

        Set<String> addedTunnelIps = Sets.difference(newTunelIps, oldTunnelIps);
        Set<String> removedTunnelIps = Sets.difference(oldTunnelIps, newTunelIps);

        InstanceIdentifier<Node> psIid = getInstanceIdentifier(newPSwitch);
        for (String tunnelIp : removedTunnelIps) {
            InstanceIdentifier<TunnelIps> tunnelIpsInstanceIdentifier = getTunnelIpIid(tunnelIp, psIid);
            transaction.delete(LogicalDatastoreType.OPERATIONAL, tunnelIpsInstanceIdentifier);
        }
        for (String tunnelIp : addedTunnelIps) {
            IpAddress ip = new IpAddress(tunnelIp.toCharArray());
            InstanceIdentifier<TunnelIps> tunnelIpsInstanceIdentifier = getTunnelIpIid(tunnelIp, psIid);
            TunnelIps tunnelIps = new TunnelIpsBuilder().setKey(new TunnelIpsKey(ip)).setTunnelIpsKey(ip).build();
            transaction.put(LogicalDatastoreType.OPERATIONAL, tunnelIpsInstanceIdentifier, tunnelIps, true);
        }
    }

    private Node buildPhysicalSwitchNode(Node node, PhysicalSwitch pSwitch) {
        NodeBuilder psNodeBuilder = new NodeBuilder();
        NodeId psNodeId = getNodeId(pSwitch);
        psNodeBuilder.setNodeId(psNodeId);
        PhysicalSwitchAugmentationBuilder psAugmentationBuilder = new PhysicalSwitchAugmentationBuilder();
        psAugmentationBuilder.setPhysicalSwitchUuid(new Uuid(pSwitch.getUuid().toString()));
        setManagedBy(psAugmentationBuilder);
        setPhysicalSwitchId(psAugmentationBuilder, pSwitch);
        setManagementIps(psAugmentationBuilder, pSwitch);
        setTunnels(psAugmentationBuilder, pSwitch);
        setSwitchFaultStatus(psAugmentationBuilder, pSwitch);

        psNodeBuilder.addAugmentation(PhysicalSwitchAugmentation.class, psAugmentationBuilder.build());

        LOG.trace("Built with the intent to store PhysicalSwitch data {}", psAugmentationBuilder.build());
        return psNodeBuilder.build();
    }

    private void setTunnels(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if (updatedTunnelRows != null && pSwitch.getTunnels() != null && pSwitch.getTunnels().getData() != null
                && !pSwitch.getTunnels().getData().isEmpty()) {
            // Nothing to do but update deviceInfo cache
            for(UUID uuid: pSwitch.getTunnels().getData()) {
                getOvsdbConnectionInstance().getDeviceInfo().putPhysicalSwitchForTunnel(uuid, pSwitch.getUuid());
            }
        }
    }

    private void setManagedBy(PhysicalSwitchAugmentationBuilder psAugmentationBuilder) {
        InstanceIdentifier<Node> connectionNodePath = getOvsdbConnectionInstance().getInstanceIdentifier();
        psAugmentationBuilder.setManagedBy(new HwvtepGlobalRef(connectionNodePath));
    }

    private void setPhysicalSwitchId(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if (pSwitch.getName() != null) {
            psAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName(pSwitch.getName()));
        }
        if (pSwitch.getDescription() != null) {
            psAugmentationBuilder.setHwvtepNodeDescription(pSwitch.getDescription());
        }
    }

    private void setManagementIps(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if (pSwitch.getManagementIpsColumn() != null && pSwitch.getManagementIpsColumn().getData() != null
                && !pSwitch.getManagementIpsColumn().getData().isEmpty()) {
            List<ManagementIps> mgmtIps = new ArrayList<>();
            for (String mgmtIp : pSwitch.getManagementIpsColumn().getData()) {
                IpAddress ip = new IpAddress(mgmtIp.toCharArray());
                mgmtIps.add(
                        new ManagementIpsBuilder().setKey(new ManagementIpsKey(ip)).setManagementIpsKey(ip).build());
            }
            psAugmentationBuilder.setManagementIps(mgmtIps);
        }
    }

    private void setTunnelIps(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if (pSwitch.getTunnelIpsColumn() != null && pSwitch.getTunnelIpsColumn().getData() != null
                && !pSwitch.getTunnelIpsColumn().getData().isEmpty()) {
            List<TunnelIps> tunnelIps = new ArrayList<>();
            for (String tunnelIp : pSwitch.getTunnelIpsColumn().getData()) {
                IpAddress ip = new IpAddress(tunnelIp.toCharArray());
                tunnelIps.add(new TunnelIpsBuilder().setKey(new TunnelIpsKey(ip)).setTunnelIpsKey(ip).build());
            }
            psAugmentationBuilder.setTunnelIps(tunnelIps);
        }
    }

    private Node buildConnectionNode(PhysicalSwitch pSwitch) {
        // Update node with PhysicalSwitch reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());

        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<Switches> switches = new ArrayList<>();
        InstanceIdentifier<Node> switchIid =
                HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), pSwitch);
        hgAugmentationBuilder.setSwitches(switches);
        Switches physicalSwitch = new SwitchesBuilder().setSwitchRef(new HwvtepPhysicalSwitchRef(switchIid)).build();
        switches.add(physicalSwitch);

        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());

        LOG.debug("Update node with physicalswitch ref {}", hgAugmentationBuilder.getSwitches().iterator().next());
        return connectionNode.build();
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(PhysicalSwitch pSwitch) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), pSwitch);
    }

    private NodeId getNodeId(PhysicalSwitch pSwitch) {
        NodeKey nodeKey = getInstanceIdentifier(pSwitch).firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    private <T extends DataObject> void deleteEntries(ReadWriteTransaction transaction,
            List<InstanceIdentifier<T>> entryIids) {
        for (InstanceIdentifier<T> entryIid : entryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, entryIid);
        }
    }

    private List<InstanceIdentifier<SwitchFaultStatus>> getSwitchFaultStatusToRemove(InstanceIdentifier<Node> psIid,
            PhysicalSwitch pSwitch) {
        Preconditions.checkNotNull(psIid);
        Preconditions.checkNotNull(pSwitch);
        List<InstanceIdentifier<SwitchFaultStatus>> result = new ArrayList<>();
        PhysicalSwitch oldSwitch = oldPSRows.get(pSwitch.getUuid());
        if (oldSwitch != null && oldSwitch.getSwitchFaultStatusColumn() != null) {
            for (String switchFltStat : oldSwitch.getSwitchFaultStatusColumn().getData()) {
                if (pSwitch.getSwitchFaultStatusColumn() == null
                        || !pSwitch.getSwitchFaultStatusColumn().getData().contains(switchFltStat)) {
                    InstanceIdentifier<SwitchFaultStatus> iid = psIid.augmentation(PhysicalSwitchAugmentation.class)
                            .child(SwitchFaultStatus.class, new SwitchFaultStatusKey(switchFltStat));
                    result.add(iid);
                }
            }
        }
        return result;
    }

    private void setSwitchFaultStatus(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if (pSwitch.getSwitchFaultStatusColumn() != null && pSwitch.getSwitchFaultStatusColumn().getData() != null
                && !pSwitch.getSwitchFaultStatusColumn().getData().isEmpty()) {
            List<SwitchFaultStatus> switchFaultStatusLst = new ArrayList<>();
            for (String switchFaultStatus : pSwitch.getSwitchFaultStatusColumn().getData()) {
                switchFaultStatusLst
                        .add(new SwitchFaultStatusBuilder().setKey(new SwitchFaultStatusKey(switchFaultStatus))
                                .setSwitchFaultStatusKey(switchFaultStatus).build());
            }
            psAugmentationBuilder.setSwitchFaultStatus(switchFaultStatusLst);
        }
    }

}
