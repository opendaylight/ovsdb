/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.Collection;
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
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class PhysicalSwitchUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PhysicalSwitchUpdateCommand.class);
    private Map<UUID, PhysicalSwitch> updatedPSRows;
    private Map<UUID, PhysicalSwitch> oldPSRows;
    private Map<UUID, Tunnel> updatedTunnelRows;
    private Map<UUID, PhysicalLocator> updatedPLocRows;
    private Collection<Tunnel> deletedTunnelRows;
    private Map<UUID, Tunnel> oldTunnelRows;
    private Map<UUID, PhysicalSwitch> deletedPSRows;

    public PhysicalSwitchUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPSRows = TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(),getDbSchema());
        oldPSRows = TyperUtils.extractRowsOld(PhysicalSwitch.class, getUpdates(),getDbSchema());
        deletedPSRows = TyperUtils.extractRowsRemoved(PhysicalSwitch.class, getUpdates(),getDbSchema());
        deletedTunnelRows = TyperUtils.extractRowsRemoved(Tunnel.class, getUpdates(),getDbSchema()).values();
        oldTunnelRows = TyperUtils.extractRowsOld(Tunnel.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        LOG.trace("Old Physical Switch Rows are {} as stated", oldPSRows.values() );
        LOG.trace("Updated Physical Switch Rows are {} as stated", updatedPSRows.values() );
        LOG.trace("Deleted Physical Switch Rows are {} as stated", deletedPSRows.values() );

        if(updatedPSRows != null && !updatedPSRows.isEmpty()) {
            for (Entry<UUID, PhysicalSwitch> entry : updatedPSRows.entrySet()) {
                updatePhysicalSwitch(transaction, entry.getValue());
            }
        }
        if(deletedTunnelRows != null && !deletedTunnelRows.isEmpty()){
            for(Tunnel tunnel : deletedTunnelRows){
                deleteTunnel(transaction, tunnel);
            }

        }
    }

    private void deleteTunnel(ReadWriteTransaction transaction, Tunnel tunnel) {
        if(oldPSRows != null && !oldPSRows.isEmpty()){
            for (PhysicalSwitch pSwitch : oldPSRows.values()){
                Set<UUID> uuidList = pSwitch.getTunnels().getData();
                for(UUID uuid : uuidList){
                    if(oldTunnelRows.get(uuid)==tunnel){
                       //TODO delete Tunnel from Operational Data Store
                    }
                }
            }
        }

    }

    private void updatePhysicalSwitch(ReadWriteTransaction transaction, PhysicalSwitch pSwitch) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present",connection);
            // Update the connection node to let it know it manages this Physical Switch
            Node connectionNode = buildConnectionNode(pSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);

            // Update the Physical Switch with whatever data we are getting
            InstanceIdentifier<Node> psIid = getInstanceIdentifier(pSwitch);
            Node psNode = buildPhysicalSwitchNode(connection.get(),pSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, psIid, psNode);
//            TODO: Delete entries that are no longer needed
        }
    }

    private Node buildPhysicalSwitchNode(Node node, PhysicalSwitch pSwitch) {
        NodeBuilder psNodeBuilder = new NodeBuilder();
        NodeId psNodeId = getNodeId(pSwitch);
        psNodeBuilder.setNodeId(psNodeId);
        PhysicalSwitchAugmentationBuilder psAugmentationBuilder = new PhysicalSwitchAugmentationBuilder();
        psAugmentationBuilder.setPhysicalSwitchUuid(new Uuid(pSwitch.getUuid().toString()));
        setManagedBy(psAugmentationBuilder, pSwitch);
        setPhysicalSwitchId(psAugmentationBuilder, pSwitch);
        setManagementIps(psAugmentationBuilder, pSwitch);
        setTunnelIps(psAugmentationBuilder, pSwitch);
        setTunnels(node, psAugmentationBuilder, pSwitch);

        psNodeBuilder.addAugmentation(PhysicalSwitchAugmentation.class, psAugmentationBuilder.build());
        LOG.trace("Built with the intent to store PhysicalSwitch data {}",
                psAugmentationBuilder.build());
        LOG.trace("Updated PS rows{} are present ", updatedPSRows.values());
        LOG.trace("Old PS rows{} are present", oldPSRows.values());
        LOG.trace("Deleted PS rows{} are present", deletedPSRows.values());
        return psNodeBuilder.build();
    }

    private void setTunnels(Node node, PhysicalSwitchAugmentationBuilder psAugmentationBuilder,
            PhysicalSwitch pSwitch){
        if(pSwitch.getTunnels()!=null && pSwitch.getTunnels().getData()!=null
                && pSwitch.getTunnels().getData().isEmpty()){
            Set<UUID> uuidList = pSwitch.getTunnels().getData();
            List<Tunnels> tunnelList = new ArrayList<>();
            TunnelsBuilder tBuilder = new TunnelsBuilder();
            for(UUID uuid : uuidList){
                Tunnel tunnel = updatedTunnelRows.get(uuid);
                if (tunnel.getLocalColumn().getData() != null){
                    PhysicalLocator pLoc = updatedPLocRows.get(tunnel.getLocalColumn().getData());
                    InstanceIdentifier<TerminationPoint> tpPath = HwvtepSouthboundMapper.createInstanceIdentifier(
                            HwvtepSouthboundMapper.createInstanceIdentifier(node.getNodeId()), pLoc);
                    tBuilder.setLocalLocatorRef(new HwvtepPhysicalLocatorRef(tpPath));
                }
                //TODO bfdconfiglocal
                //TODO bfdconfigremote
                //TODO bfdparams
                if (tunnel.getRemoteColumn().getData() != null){
                    PhysicalLocator pLoc = updatedPLocRows.get(tunnel.getRemoteColumn().getData());
                    InstanceIdentifier<TerminationPoint> tpPath = HwvtepSouthboundMapper.createInstanceIdentifier(
                            HwvtepSouthboundMapper.createInstanceIdentifier(node.getNodeId()), pLoc);
                    tBuilder.setRemoteLocatorRef(new HwvtepPhysicalLocatorRef(tpPath));
                }
                tunnelList.add(tBuilder.build());
            }
            psAugmentationBuilder.setTunnels(tunnelList);
        }

    }

    private void setManagedBy(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        InstanceIdentifier<Node> connectionNodePath = getOvsdbConnectionInstance().getInstanceIdentifier();
        psAugmentationBuilder.setManagedBy(new HwvtepGlobalRef(connectionNodePath));
    }

    private void setPhysicalSwitchId(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if(pSwitch.getName() != null) {
            psAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName(pSwitch.getName()));
        }
        if(pSwitch.getDescription() != null) {
            psAugmentationBuilder.setHwvtepNodeDescription(pSwitch.getDescription());
        }
    }

    private void setManagementIps(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if(pSwitch.getManagementIpsColumn() != null
                && pSwitch.getManagementIpsColumn().getData() != null
                && !pSwitch.getManagementIpsColumn().getData().isEmpty() ) {
            List<ManagementIps> mgmtIps = new ArrayList<>();
            for(String mgmtIp: pSwitch.getManagementIpsColumn().getData()) {
                IpAddress ip = new IpAddress(mgmtIp.toCharArray());
                mgmtIps.add(new ManagementIpsBuilder()
                        .setKey(new ManagementIpsKey(ip))
                        .setManagementIpsKey(ip)
                        .build());
            }
            psAugmentationBuilder.setManagementIps(mgmtIps);
        }
    }

    private void setTunnelIps(PhysicalSwitchAugmentationBuilder psAugmentationBuilder, PhysicalSwitch pSwitch) {
        if(pSwitch.getTunnelIpsColumn() != null
            && pSwitch.getTunnelIpsColumn().getData() != null
            && !pSwitch.getTunnelIpsColumn().getData().isEmpty()) {
            List<TunnelIps> tunnelIps = new ArrayList<>();
            for(String tunnelIp: pSwitch.getTunnelIpsColumn().getData()) {
                IpAddress ip = new IpAddress(tunnelIp.toCharArray());
                tunnelIps.add(new TunnelIpsBuilder()
                        .setKey(new TunnelIpsKey(ip))
                        .setTunnelIpsKey(ip)
                        .build());
            }
            psAugmentationBuilder.setTunnelIps(tunnelIps);
        }
    }

    private Node buildConnectionNode(PhysicalSwitch pSwitch) {
        //Update node with PhysicalSwitch reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());

        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<Switches> switches = new ArrayList<>();
        InstanceIdentifier<Node> switchIid = HwvtepSouthboundMapper
                .createInstanceIdentifier(getOvsdbConnectionInstance(),pSwitch);
        hgAugmentationBuilder.setSwitches(switches);
        Switches physicalSwitch = new SwitchesBuilder().setSwitchRef(
                        new HwvtepPhysicalSwitchRef(switchIid)).build();
        switches.add(physicalSwitch);

        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());

        LOG.debug("Update node with physicalswitch ref {}",
                hgAugmentationBuilder.getSwitches().iterator().next());
        return connectionNode.build();
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(PhysicalSwitch pSwitch) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                pSwitch);
    }

    private NodeId getNodeId(PhysicalSwitch pSwitch) {
        NodeKey nodeKey = getInstanceIdentifier(pSwitch).firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }
}
