/*
 * Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbPortUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbPortUpdateCommand.class);

    public OvsdbPortUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        String bridgeName = null;
        Collection<Port> portUpdatedRows = TyperUtils.extractRowsUpdated(
                Port.class, getUpdates(), getDbSchema()).values();
        Collection<Bridge> bridgeUpdatedRows = TyperUtils.extractRowsUpdated(
                Bridge.class, getUpdates(), getDbSchema()).values();
        for (Bridge bridge : bridgeUpdatedRows) {
            Iterator<UUID> bridgePorts = bridge.getPortsColumn().getData()
                    .iterator();
            while (bridgePorts.hasNext()) {
                UUID portUUID = bridgePorts.next();
                for (Port port : portUpdatedRows) {
                    if (portUUID.equals(port.getUuid())) {
                        bridgeName = bridge.getName();
                        NodeId bridgeId = SouthboundMapper.createManagedNodeId(
                                getKey(), new OvsdbBridgeName(bridgeName));
                        final InstanceIdentifier<Node> nodePath = SouthboundMapper
                                .createInstanceIdentifier(bridgeId);
                        Optional<Node> node = readNode(transaction, nodePath);
                        if (node.isPresent()) {
                            NodeBuilder nodeBuilder = buildNode(node, bridgeId, bridge, port);
                            transaction.merge(LogicalDatastoreType.OPERATIONAL,
                                    nodePath, nodeBuilder.build());
                        }
                    }
                }
            }
        }
    }

    private Optional<Node> readNode(final ReadWriteTransaction transaction, final InstanceIdentifier<Node> nodePath) {
        Optional<Node> node = Optional.absent();
        try {
            node = transaction.read(
                    LogicalDatastoreType.OPERATIONAL, nodePath)
                    .checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Read Operational/DS for Node fail! {}",
                    nodePath, e);
        }
        return node;
    }

    private NodeBuilder buildNode(final Optional<Node> node, final NodeId bridgeId, final Bridge bridge,
            final Port port) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(bridgeId);
        List<TerminationPoint> tpList = buildTpList(bridge, port);
        nodeBuilder.setTerminationPoint(tpList);
        nodeBuilder.addAugmentation(
                OvsdbBridgeAugmentation.class,
                node.get().getAugmentation(
                        OvsdbBridgeAugmentation.class));
        return nodeBuilder;
    }

    private List<TerminationPoint> buildTpList(final Bridge bridge, final Port port) {
        List<TerminationPoint> tpList = new ArrayList<TerminationPoint>();
        TerminationPointBuilder entry = new TerminationPointBuilder();
        entry.setTpId(new TpId(port.getName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointBuilder
                .setName(port.getName());
        ovsdbTerminationPointBuilder.setPortUuid(new Uuid(
                port.getUuid().toString()));
        updatePort(port, ovsdbTerminationPointBuilder);
        updateInterfaces(port, bridge, ovsdbTerminationPointBuilder);
        entry.addAugmentation(
                OvsdbTerminationPointAugmentation.class,
                ovsdbTerminationPointBuilder.build());
        return tpList;
    }

    private void updateInterfaces(final Port port, final Bridge bridge,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Column<GenericTableSchema, Set<UUID>> iface = port.getInterfacesColumn();
        Set<UUID> ifUuid = iface.getData();
        Collection<Interface> ifUpdateRows = TyperUtils.extractRowsUpdated(
                Interface.class, getUpdates(),  getDbSchema()).values();
        updateInterfaces(ifUuid, ifUpdateRows, bridge, ovsdbTerminationPointBuilder);
    }

    private void updateInterfaces(final Set<UUID> ifUuid, final Collection<Interface> ifUpdateRows,
            final Bridge bridge, final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        for (UUID ifIter : ifUuid) {
            for (Interface interfIter : ifUpdateRows) {
                Column<GenericTableSchema, String> typeColumn = interfIter.getTypeColumn();
                String type = typeColumn.getData();
                if ((interfIter.getUuid()).equals(ifIter)) {
                    updateInterface(interfIter, bridge, type, ovsdbTerminationPointBuilder);
                    break;
                }
            }
        }
    }

    private void updatePort(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        updateVlan(port, ovsdbTerminationPointBuilder);
        updateVlanTrunks(port, ovsdbTerminationPointBuilder);
        updateVlanMode(port, ovsdbTerminationPointBuilder);
        updatePortExternalIds(port, ovsdbTerminationPointBuilder);
        updatePortOtherConfig(port, ovsdbTerminationPointBuilder);
    }

    private void updateInterface(final Interface interf,
            final Bridge bridge, final String type,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        ovsdbTerminationPointBuilder.setInterfaceUuid(
                new Uuid(interf.getUuid().toString()));
        ovsdbTerminationPointBuilder.setInterfaceType(
                SouthboundMapper.createInterfaceType(type));
        updateOfPort(interf, bridge, ovsdbTerminationPointBuilder);
        updateOfPortRequest(interf, bridge, ovsdbTerminationPointBuilder);
        updateInterfaceExternalIds(interf, ovsdbTerminationPointBuilder);
        updateOptions(interf, ovsdbTerminationPointBuilder);
        updateInterfaceOtherConfig(interf, ovsdbTerminationPointBuilder);
    }

    private void updateVlan(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Collection<Long> vlanId = port.getTagColumn().getData();
        if (vlanId.size() > 0) {
            Iterator<Long> itr = vlanId.iterator();
            // There are no loops here, just get the first element.
            int id = itr.next().intValue();
            ovsdbTerminationPointBuilder.setVlanTag(new VlanId(id));
        }
    }

    private void updateVlanTrunks(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Set<Long> portTrunks = port.getTrunksColumn().getData();
        List<Trunks> modelTrunks = new ArrayList<Trunks>();
        if (!portTrunks.isEmpty()) {
            for (Long trunk: portTrunks) {
                if (trunk != null) {
                    modelTrunks.add(new TrunksBuilder()
                        .setTrunk(new VlanId(trunk.intValue())).build());
                }
            }
            ovsdbTerminationPointBuilder.setTrunks(modelTrunks);
        }
    }

    private void updateVlanMode(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Collection<String> vlanMode = port.getVlanModeColumn().getData();
        if (!vlanMode.isEmpty()) {
            Iterator<String> itr = vlanMode.iterator();
            String vlanType = itr.next();
            if (vlanType.equals(SouthboundConstants.VLANMODES.ACCESS.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access);
            } else if (vlanType.equals(SouthboundConstants.VLANMODES.NATIVE_TAGGED.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.NativeTagged);
            } else if (vlanType.equals(SouthboundConstants.VLANMODES.NATIVE_UNTAGGED.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.NativeUntagged);
            } else if (vlanType.equals(SouthboundConstants.VLANMODES.TRUNK.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Trunk);
            } else {
                LOG.debug("Invalid vlan mode {}.", vlanType);
            }
        }
    }

    private void updateOfPort(final Interface interf,
            final Bridge bridge,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Set<Long> ofPorts = interf.getOpenFlowPortColumn().getData();
        if (ofPorts != null && !ofPorts.isEmpty()) {
            Iterator<Long> ofPortsIter = ofPorts.iterator();
            long ofPort = ofPortsIter.next();
            if (ofPort >= 0) {
                ovsdbTerminationPointBuilder
                    .setOfport(ofPort);
            } else {
                LOG.debug("Received negative value for ofPort from ovsdb for {} {} {}",
                        bridge.getName(), interf.getName(),ofPort);
            }
        }
    }

    private void updateOfPortRequest(final Interface interf,
            final Bridge bridge,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Set<Long> ofPortRequests = interf
                .getOpenFlowPortRequestColumn().getData();
        if (ofPortRequests != null && !ofPortRequests.isEmpty()) {
            Iterator<Long> ofPortRequestsIter = ofPortRequests.iterator();
            int ofPort = ofPortRequestsIter.next().intValue();
            if (ofPort >= 0) {
                ovsdbTerminationPointBuilder
                    .setOfportRequest(ofPort);
            } else {
                LOG.debug("Received negative value for ofPort from ovsdb for {} {} {}",
                        bridge.getName(), interf.getName(),ofPort);
            }
        }
    }

    private void updateInterfaceExternalIds(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> interfaceExternalIds =
                interf.getExternalIdsColumn().getData();
        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            Set<String> externalIdKeys = interfaceExternalIds.keySet();
            List<InterfaceExternalIds> externalIdsList =
                    new ArrayList<InterfaceExternalIds>();
            String externalIdValue;
            for (String externalIdKey : externalIdKeys) {
                externalIdValue = interfaceExternalIds.get(externalIdKey);
                if (externalIdKey != null && externalIdValue != null) {
                    externalIdsList.add(new InterfaceExternalIdsBuilder()
                            .setExternalIdKey(externalIdKey)
                            .setExternalIdValue(externalIdValue).build());
                }
            }
            ovsdbTerminationPointBuilder.setInterfaceExternalIds(externalIdsList);
        }
    }

    private void updatePortExternalIds(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> portExternalIds = port.getExternalIdsColumn().getData();
        if (portExternalIds != null && !portExternalIds.isEmpty()) {
            Set<String> externalIdKeys = portExternalIds.keySet();
            List<PortExternalIds> externalIdsList = new ArrayList<PortExternalIds>();
            String externalIdValue;
            for (String externalIdKey : externalIdKeys) {
                externalIdValue = portExternalIds.get(externalIdKey);
                if (externalIdKey != null && externalIdValue != null) {
                    externalIdsList.add(new PortExternalIdsBuilder()
                            .setExternalIdKey(externalIdKey)
                            .setExternalIdValue(externalIdValue).build());
                }
            }
            ovsdbTerminationPointBuilder.setPortExternalIds(externalIdsList);
        }
    }

    private void updateOptions(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> optionsMap = interf.getOptionsColumn().getData();
        if (optionsMap != null && !optionsMap.isEmpty()) {
            List<Options> options = new ArrayList<Options>();
            String optionsValueString;
            OptionsKey optionsKey;
            for (String optionsKeyString : optionsMap.keySet()) {
                optionsValueString = optionsMap.get(optionsKeyString);
                if (optionsKeyString != null && optionsValueString != null) {
                    optionsKey = new OptionsKey(optionsKeyString);
                    options.add(new OptionsBuilder()
                        .setKey(optionsKey)
                        .setValue(optionsValueString).build());
                }
            }
            ovsdbTerminationPointBuilder.setOptions(options);
        }
    }

    private void updatePortOtherConfig(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> portOtherConfigMap = port.getOtherConfigColumn().getData();
        if (portOtherConfigMap != null && !portOtherConfigMap.isEmpty()) {
            List<PortOtherConfigs> portOtherConfigs = new ArrayList<PortOtherConfigs>();
            String portOtherConfigValueString;
            for (String portOtherConfigKeyString : portOtherConfigMap.keySet()) {
                portOtherConfigValueString = portOtherConfigMap.get(portOtherConfigKeyString);
                if (portOtherConfigKeyString != null && portOtherConfigValueString != null) {
                    portOtherConfigs.add(new PortOtherConfigsBuilder()
                        .setOtherConfigKey(portOtherConfigKeyString)
                        .setOtherConfigValue(portOtherConfigValueString).build());
                }
            }
            ovsdbTerminationPointBuilder.setPortOtherConfigs(portOtherConfigs);
        }
    }

    private void updateInterfaceOtherConfig(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> interfaceOtherConfigMap = interf.getOtherConfigColumn().getData();
        if (interfaceOtherConfigMap != null && !interfaceOtherConfigMap.isEmpty()) {
            List<InterfaceOtherConfigs> interfaceOtherConfigs = new ArrayList<InterfaceOtherConfigs>();
            String interfaceOtherConfigValueString;
            for (String interfaceOtherConfigKeyString : interfaceOtherConfigMap.keySet()) {
                interfaceOtherConfigValueString = interfaceOtherConfigMap.get(interfaceOtherConfigKeyString);
                if (interfaceOtherConfigKeyString != null && interfaceOtherConfigValueString != null) {
                    interfaceOtherConfigs.add(new InterfaceOtherConfigsBuilder()
                        .setOtherConfigKey(interfaceOtherConfigKeyString)
                        .setOtherConfigValue(interfaceOtherConfigValueString).build());
                }
            }
            ovsdbTerminationPointBuilder.setInterfaceOtherConfigs(interfaceOtherConfigs);
        }
    }
}
