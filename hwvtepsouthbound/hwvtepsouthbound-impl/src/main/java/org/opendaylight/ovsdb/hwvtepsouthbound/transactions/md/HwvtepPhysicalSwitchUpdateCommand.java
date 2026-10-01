/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactUtils;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.SwitchFaultStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.SwitchFaultStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.SwitchFaultStatusKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepPhysicalSwitchUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepPhysicalSwitchUpdateCommand.class);

    private final Map<UUID, PhysicalSwitch> updatedPSRows;
    private Map<UUID, Tunnel> updatedTunnelRows;
    private final Map<UUID, PhysicalSwitch> oldPSRows;

    public HwvtepPhysicalSwitchUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPSRows = TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(), getDbSchema());
        oldPSRows = TyperUtils.extractRowsOld(PhysicalSwitch.class, getUpdates(), getDbSchema());
        try {
            updatedTunnelRows = TyperUtils.extractRowsUpdated(Tunnel.class, getUpdates(), getDbSchema());
        } catch (IllegalArgumentException e) {
            LOG.debug("Tunnel Table not supported on this HWVTEP device", e);
        }
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Map.Entry<UUID, PhysicalSwitch> entry : updatedPSRows.entrySet()) {
            updatePhysicalSwitch(transaction, entry.getKey(), entry.getValue());
        }
    }

    private void updatePhysicalSwitch(ReadWriteTransaction transaction, UUID uuid, PhysicalSwitch phySwitch) {
        final var connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        //TODO remove this read
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present", connection);
            // Update the connection node to let it know it manages this
            // Physical Switch
            Node connectionNode = buildConnectionNode(phySwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);

            // Update the Physical Switch with whatever data we are getting
            var psIid = getInstanceIdentifier(phySwitch);
            Node psNode = buildPhysicalSwitchNode(connection.orElseThrow(), phySwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, psIid, psNode);
            addToDeviceUpdate(TransactionType.ADD, phySwitch);
            LOG.info("DEVICE - {} {}", TransactionType.ADD, phySwitch);

            PhysicalSwitch oldPSwitch = oldPSRows.get(uuid);
            updateTunnelIps(phySwitch, oldPSwitch, transaction);

            getOvsdbConnectionInstance().getDeviceInfo().putPhysicalSwitch(phySwitch.getUuid(), phySwitch);
            getDeviceInfo().updateDeviceOperData(Node.class, psIid, phySwitch.getUuid(), phySwitch);
            // TODO: Delete entries that are no longer needed
            // TODO: Deletion of tunnels
            // TODO: Deletion of Tunnel BFD config and params
            //Deleting old switch fault status entries
            deleteEntries(transaction, getSwitchFaultStatusToRemove(psIid,phySwitch));
        }
    }

    private static DataObjectIdentifier<TunnelIps> getTunnelIpIid(final String tunnelIp,
            final DataObjectIdentifier<Node> psIid) {
        return psIid.toBuilder()
            .augmentation(PhysicalSwitchAugmentation.class)
            .child(TunnelIps.class, new TunnelIpsKey(TransactUtils.parseIpAddress(tunnelIp)))
            .build();
    }

    private void updateTunnelIps(@NonNull final PhysicalSwitch newPSwitch, @Nullable final PhysicalSwitch oldPSwitch,
                                 final ReadWriteTransaction transaction) {
        Set<String> oldTunnelIps = oldPSwitch != null && oldPSwitch.getTunnelIpsColumn() != null ? oldPSwitch
                .getTunnelIpsColumn().getData() : Collections.emptySet();
        Set<String> newTunelIps = newPSwitch.getTunnelIpsColumn() != null ? newPSwitch
                .getTunnelIpsColumn().getData() : Collections.emptySet();

        Set<String> addedTunnelIps = Sets.difference(newTunelIps, oldTunnelIps);
        Set<String> removedTunnelIps = Sets.difference(oldTunnelIps, newTunelIps);

        var psIid = getInstanceIdentifier(newPSwitch);
        for (String tunnelIp : removedTunnelIps) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, getTunnelIpIid(tunnelIp, psIid));
        }
        for (String tunnelIp : addedTunnelIps) {
            IpAddress ip = TransactUtils.parseIpAddress(tunnelIp);
            var tunnelIpsInstanceIdentifier = getTunnelIpIid(tunnelIp, psIid);
            TunnelIps tunnelIps = new TunnelIpsBuilder().withKey(new TunnelIpsKey(ip)).setTunnelIpsKey(ip).build();
            transaction.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL,
                tunnelIpsInstanceIdentifier, tunnelIps);
        }
    }

    private Node buildPhysicalSwitchNode(Node node, PhysicalSwitch phySwitch) {
        NodeBuilder psNodeBuilder = new NodeBuilder();
        NodeId psNodeId = getNodeId(phySwitch);
        psNodeBuilder.setNodeId(psNodeId);
        PhysicalSwitchAugmentationBuilder psAugmentationBuilder = new PhysicalSwitchAugmentationBuilder();
        psAugmentationBuilder.setPhysicalSwitchUuid(new Uuid(phySwitch.getUuid().toString()));
        setManagedBy(psAugmentationBuilder);
        setPhysicalSwitchId(psAugmentationBuilder, phySwitch);
        setManagementIps(psAugmentationBuilder, phySwitch);
        setTunnels(psAugmentationBuilder, phySwitch);
        setSwitchFaultStatus(psAugmentationBuilder, phySwitch);

        psNodeBuilder.addAugmentation(psAugmentationBuilder.build());

        LOG.trace("Built with the intent to store PhysicalSwitch data {}", psAugmentationBuilder.build());
        return psNodeBuilder.build();
    }

    private void setTunnels(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch phySwitch) {
        if (updatedTunnelRows != null && phySwitch.getTunnels() != null && phySwitch.getTunnels().getData() != null
                && !phySwitch.getTunnels().getData().isEmpty()) {
            // Nothing to do but update deviceInfo cache
            for (UUID uuid: phySwitch.getTunnels().getData()) {
                getOvsdbConnectionInstance().getDeviceInfo().putPhysicalSwitchForTunnel(uuid, phySwitch.getUuid());
            }
        }
    }

    private void setManagedBy(PhysicalSwitchAugmentationBuilder psAugmentationBuilder) {
        psAugmentationBuilder.setManagedBy(new HwvtepGlobalRef(getOvsdbConnectionInstance().getInstanceIdentifier()));
    }

    private static void setPhysicalSwitchId(PhysicalSwitchAugmentationBuilder psAugmentationBuilder,
            PhysicalSwitch phySwitch) {
        if (phySwitch.getName() != null) {
            psAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName(phySwitch.getName()));
        }
        if (phySwitch.getDescription() != null) {
            psAugmentationBuilder.setHwvtepNodeDescription(phySwitch.getDescription());
        }
    }

    private static void setManagementIps(PhysicalSwitchAugmentationBuilder psAugmentationBuilder,
            PhysicalSwitch phySwitch) {
        if (phySwitch.getManagementIpsColumn() != null && phySwitch.getManagementIpsColumn().getData() != null
                && !phySwitch.getManagementIpsColumn().getData().isEmpty()) {
            var mgmtIps = phySwitch.getManagementIpsColumn().getData().stream()
                .map(ip -> new ManagementIpsBuilder()
                    .setManagementIpsKey(TransactUtils.parseIpAddress(ip))
                    .build())
                .collect(BindingMap.toMap());
            psAugmentationBuilder.setManagementIps(mgmtIps);
        }
    }

    private Node buildConnectionNode(PhysicalSwitch phySwitch) {
        // Update node with PhysicalSwitch reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());

        var switchIid = HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), phySwitch);
        Switches physicalSwitch = new SwitchesBuilder().setSwitchRef(new HwvtepPhysicalSwitchRef(switchIid)).build();

        connectionNode.addAugmentation(new HwvtepGlobalAugmentationBuilder()
            .setSwitches(BindingMap.of(physicalSwitch))
            .build());

        LOG.debug("Update node with physicalswitch ref {}", physicalSwitch);
        return connectionNode.build();
    }

    private DataObjectIdentifier<Node> getInstanceIdentifier(PhysicalSwitch phySwitch) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), phySwitch);
    }

    private NodeId getNodeId(PhysicalSwitch phySwitch) {
        return getInstanceIdentifier(phySwitch).getFirstKeyOf(Node.class).getNodeId();
    }

    private static <T extends DataObject> void deleteEntries(ReadWriteTransaction transaction,
            List<DataObjectIdentifier<T>> entryIids) {
        for (DataObjectIdentifier<T> entryIid : entryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, entryIid);
        }
    }

    private List<DataObjectIdentifier<SwitchFaultStatus>> getSwitchFaultStatusToRemove(DataObjectIdentifier<Node> psIid,
            PhysicalSwitch phySwitch) {
        requireNonNull(psIid);
        requireNonNull(phySwitch);
        List<DataObjectIdentifier<SwitchFaultStatus>> result = new ArrayList<>();
        PhysicalSwitch oldSwitch = oldPSRows.get(phySwitch.getUuid());
        if (oldSwitch != null && oldSwitch.getSwitchFaultStatusColumn() != null) {
            for (String switchFltStat : oldSwitch.getSwitchFaultStatusColumn().getData()) {
                if (phySwitch.getSwitchFaultStatusColumn() == null
                        || !phySwitch.getSwitchFaultStatusColumn().getData().contains(switchFltStat)) {
                    result.add(psIid.toBuilder()
                        .augmentation(PhysicalSwitchAugmentation.class)
                        .child(SwitchFaultStatus.class, new SwitchFaultStatusKey(switchFltStat))
                        .build());
                }
            }
        }
        return result;
    }

    private static void setSwitchFaultStatus(PhysicalSwitchAugmentationBuilder psAugmentationBuilder,
            PhysicalSwitch phySwitch) {
        if (phySwitch.getSwitchFaultStatusColumn() != null && phySwitch.getSwitchFaultStatusColumn().getData() != null
                && !phySwitch.getSwitchFaultStatusColumn().getData().isEmpty()) {
            var switchFaultStatusLst = phySwitch.getSwitchFaultStatusColumn().getData().stream()
                .map(switchFaultStatus -> new SwitchFaultStatusBuilder()
                    .setSwitchFaultStatusKey(switchFaultStatus)
                    .build())
                .collect(BindingMap.toOrderedMap());
            psAugmentationBuilder.setSwitchFaultStatus(switchFaultStatusLst);
        }
    }
}
