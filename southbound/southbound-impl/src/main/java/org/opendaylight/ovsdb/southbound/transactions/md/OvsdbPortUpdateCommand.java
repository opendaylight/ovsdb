/*
 * Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.error.ColumnSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortQos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortQosBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.port.qos.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.port.qos.QueuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.port.qos.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.port.qos.queues.QueueValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.port.qos.queues.QueueValueBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbPortUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbPortUpdateCommand.class);
    private Map<UUID, Port> portUpdatedRows;
    private Map<UUID, Port> portOldRows;
    private Map<UUID, Interface> interfaceUpdatedRows;
    private Map<UUID, Interface> interfaceOldRows;
    private Map<UUID, Bridge> bridgeUpdatedRows;
    private Map<UUID, Qos> portQosUpdatedRows;
    private Map<UUID, Queue> portQueueUpdatedRows;
    public OvsdbPortUpdateCommand(OvsdbConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        portUpdatedRows = TyperUtils.extractRowsUpdated(Port.class, updates, dbSchema);
        portOldRows = TyperUtils.extractRowsOld(Port.class, updates, dbSchema);
        interfaceUpdatedRows = TyperUtils.extractRowsUpdated(Interface.class, updates, dbSchema);
        interfaceOldRows = TyperUtils.extractRowsOld(Interface.class, updates, dbSchema);
        bridgeUpdatedRows = TyperUtils.extractRowsUpdated(Bridge.class, updates, dbSchema);
        portQosUpdatedRows = TyperUtils.extractRowsUpdated(Qos.class, updates, dbSchema);
        portQueueUpdatedRows = TyperUtils.extractRowsUpdated(Queue.class, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if ( (portUpdatedRows == null && interfaceOldRows == null )
                || ( interfaceOldRows.isEmpty() && portUpdatedRows.isEmpty())) {
            return;
        }
        Optional<Node> node = readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateTerminationPoints(transaction, node.get());
        }
    }
    private void updateTerminationPoints(ReadWriteTransaction transaction, Node node) {
        for (Entry<UUID, Port> portUpdate : portUpdatedRows.entrySet()) {
            String portName = null;
            portName = portUpdate.getValue().getNameColumn().getData();
            Optional<InstanceIdentifier<Node>> bridgeIid = getTerminationPointBridge(portUpdate.getKey());
            if (!bridgeIid.isPresent()) {
                bridgeIid = getTerminationPointBridge( transaction, node, portName);
            }
            if (bridgeIid.isPresent()) {
                NodeId bridgeId = SouthboundMapper.createManagedNodeId(bridgeIid.get());
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                InstanceIdentifier<TerminationPoint> tpPath =
                        getInstanceIdentifier(bridgeIid.get(), portUpdate.getValue());
                OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder,portUpdate.getValue());
                UUID interfaceUUID = (UUID)portUpdate.getValue().getInterfacesColumn().getData().toArray()[0];
                if (interfaceUpdatedRows.containsKey(interfaceUUID)) {
                    buildTerminationPoint(tpAugmentationBuilder,
                            interfaceUpdatedRows.get(interfaceUUID));
                    interfaceUpdatedRows.remove(interfaceUUID);
                    interfaceOldRows.remove(interfaceUUID);
                }
                tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
                if (portOldRows.containsKey(portUpdate.getKey())) {
                    transaction.merge(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                } else {
                    transaction.put(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                }
            }
        }
        for (Entry<UUID, Interface> interfaceUpdate : interfaceUpdatedRows.entrySet()) {
            String interfaceName = null;
            interfaceName = interfaceUpdatedRows.get(interfaceUpdate.getKey()).getNameColumn().getData();
            Optional<InstanceIdentifier<Node>> bridgeIid = getTerminationPointBridge( transaction, node, interfaceName);
            if (bridgeIid.isPresent()) {
                NodeId bridgeId = SouthboundMapper.createManagedNodeId(bridgeIid.get());
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(interfaceName));
                InstanceIdentifier<TerminationPoint> tpPath = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class,new NodeKey(bridgeId))
                        .child(TerminationPoint.class,tpKey);
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder, interfaceUpdate.getValue());
                tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        tpPath, tpBuilder.build());
            }
        }

    }
    private void buildTerminationPoint(OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder,
            Port portUpdate) {

        tpAugmentationBuilder
                .setName(portUpdate.getName());
        tpAugmentationBuilder.setPortUuid(new Uuid(
                portUpdate.getUuid().toString()));
        updatePort(portUpdate, tpAugmentationBuilder);
    }

    private void buildTerminationPoint(OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder,
            Interface interfaceUpdate) {

        tpAugmentationBuilder
                .setName(interfaceUpdate.getName());
        tpAugmentationBuilder.setInterfaceUuid(new Uuid(
                interfaceUpdate.getUuid().toString()));
        updateInterfaces(interfaceUpdate, tpAugmentationBuilder);
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

    private Optional<InstanceIdentifier<Node>> getTerminationPointBridge( UUID portUUID) {
        for (UUID bridgeUUID : this.bridgeUpdatedRows.keySet()) {
            if (this.bridgeUpdatedRows.get(bridgeUUID).getPortsColumn().getData().contains(portUUID)) {
                return Optional.of(SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                        this.bridgeUpdatedRows.get(bridgeUUID)));
            }
        }
        return Optional.absent();
    }
    private Optional<InstanceIdentifier<Node>> getTerminationPointBridge(
            final ReadWriteTransaction transaction, Node node, String tpName) {
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        List<ManagedNodeEntry> managedNodes = ovsdbNode.getManagedNodeEntry();
        for ( ManagedNodeEntry managedNodeEntry : managedNodes ) {
            @SuppressWarnings("unchecked")
            Node managedNode = readNode(transaction
                    ,(InstanceIdentifier<Node>)managedNodeEntry.getBridgeRef().getValue()).get();
            TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
            TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpName));
            tpBuilder.setKey(tpKey);
            if (managedNode.getTerminationPoint().contains(tpBuilder.build())) {
                OvsdbBridgeAugmentation ovsdbNodeAugment
                    = managedNode.getAugmentation(OvsdbBridgeAugmentation.class);
                return Optional.of((InstanceIdentifier<Node>)managedNodeEntry.getBridgeRef().getValue());
            }
        }
        return Optional.absent();
    }

    private void updateInterfaces(Interface interfaceUpdate,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Column<GenericTableSchema, String> typeColumn = interfaceUpdate.getTypeColumn();
        String type = typeColumn.getData();
        updateInterface(interfaceUpdate, type,ovsdbTerminationPointBuilder);
    }

    private void updatePort(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        updateVlan(port, ovsdbTerminationPointBuilder);
        updateVlanTrunks(port, ovsdbTerminationPointBuilder);
        updateVlanMode(port, ovsdbTerminationPointBuilder);
        updatePortExternalIds(port, ovsdbTerminationPointBuilder);
        updatePortOtherConfig(port, ovsdbTerminationPointBuilder);
        updatePortQos(port, ovsdbTerminationPointBuilder);
    }

    private void updateInterface(final Interface interf,
            final String type,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        ovsdbTerminationPointBuilder.setInterfaceUuid(
                new Uuid(interf.getUuid().toString()));
        ovsdbTerminationPointBuilder.setInterfaceType(
                SouthboundMapper.createInterfaceType(type));
        updateOfPort(interf, ovsdbTerminationPointBuilder);
        updateOfPortRequest(interf, ovsdbTerminationPointBuilder);
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
                        interf.getName(),ofPort);
            }
        }
    }

    private void updateOfPortRequest(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Set<Long> ofPortRequests = null;
        try {
            ofPortRequests = interf.getOpenFlowPortRequestColumn().getData();
        } catch (ColumnSchemaNotFoundException e) {
            LOG.warn("Cannot find openflow column", e);
        }
        if (ofPortRequests != null && !ofPortRequests.isEmpty()) {
            Iterator<Long> ofPortRequestsIter = ofPortRequests.iterator();
            int ofPort = ofPortRequestsIter.next().intValue();
            if (ofPort >= 0) {
                ovsdbTerminationPointBuilder
                    .setOfportRequest(ofPort);
            } else {
                LOG.debug("Received negative value for ofPort from ovsdb for {} {} {}",
                        interf.getName(),ofPort);
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

    private void updatePortQos(
            final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        Set<UUID> qosUuid = port.getQosColumn().getData();
        if (qosUuid != null && !qosUuid.isEmpty()) {
            List<PortQos> updatedQos = new ArrayList<PortQos>();
            for (Entry<UUID, Qos> libQos : portQosUpdatedRows.entrySet()) {
                if (libQos.getKey().equals(qosUuid)) {
                    Map<Long, UUID> libQueues = libQos.getValue().getQueuesColumn().getData();
                    List<Queues> modelQueues = new ArrayList<Queues>();
                    for (Map.Entry<Long, UUID> libQueue: libQueues.entrySet()) {
                        Queue queueData = portQueueUpdatedRows.get(libQueue.getValue());
                        QueueValue queueValue = new QueueValueBuilder()
                                                      .setDscp(queueData.getDscpColumn()
                                                                .getData().iterator()
                                                                .next().shortValue())
                                                      .setQueueUuid(new Uuid(queueData.getUuid().toString()))
                                                      //.setQueuesOtherConfig(value)
                                                      .build();
                        List<QueueValue> modelQueueValue = new ArrayList<QueueValue>();
                        modelQueueValue.add(queueValue);
                        Queues queue = new QueuesBuilder()
                                            .setKey(new QueuesKey(BigInteger.valueOf(libQueue.getKey())))
                                            .setQueueValue(modelQueueValue)
                                            .build();
                        modelQueues.add(queue);
                    }
                    PortQos modelQos = new PortQosBuilder()
                                            .setQueues(modelQueues)
                                            //.setQosExternalIds(value)
                                            //.setQosExternalIds(value)
                                            .build();
                    updatedQos.add(modelQos);
                }
            }
            ovsdbTerminationPointBuilder.setPortQos(updatedQos);
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

    private InstanceIdentifier<TerminationPoint> getInstanceIdentifier(InstanceIdentifier<Node> bridgeIid,Port port) {
        if (port.getExternalIdsColumn() != null
                && port.getExternalIdsColumn().getData() != null
                && port.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = port.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            return (InstanceIdentifier<TerminationPoint>) SouthboundUtil.deserializeInstanceIdentifier(iidString);
        } else {
            return bridgeIid.child(TerminationPoint.class, new TerminationPointKey(new TpId(port.getName())));
        }
    }
}
