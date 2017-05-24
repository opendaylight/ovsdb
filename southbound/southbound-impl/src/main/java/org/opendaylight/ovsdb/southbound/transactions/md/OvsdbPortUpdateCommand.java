/*
 * Copyright (c) 2014, 2017 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.base.Optional;
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
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
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
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQosRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatusKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
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

public class OvsdbPortUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbPortUpdateCommand.class);

    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final Map<UUID, Port> portUpdatedRows;
    private final Map<UUID, Port> portOldRows;
    private final Map<UUID, Interface> interfaceUpdatedRows;
    private final Map<UUID, Interface> interfaceOldRows;
    private final Map<UUID, Bridge> bridgeUpdatedRows;
    private final Map<UUID, Qos> qosUpdatedRows;

    public OvsdbPortUpdateCommand(InstanceIdentifierCodec instanceIdentifierCodec, OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        portUpdatedRows = TyperUtils.extractRowsUpdated(Port.class, updates, dbSchema);
        portOldRows = TyperUtils.extractRowsOld(Port.class, updates, dbSchema);
        interfaceUpdatedRows = TyperUtils.extractRowsUpdated(Interface.class, updates, dbSchema);
        interfaceOldRows = TyperUtils.extractRowsOld(Interface.class, updates, dbSchema);
        bridgeUpdatedRows = TyperUtils.extractRowsUpdated(Bridge.class, updates, dbSchema);
        qosUpdatedRows = TyperUtils.extractRowsUpdated(Qos.class, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if (portUpdatedRows == null && interfaceOldRows == null
                || interfaceOldRows.isEmpty() && portUpdatedRows.isEmpty()) {
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
                bridgeIid = getTerminationPointBridge(transaction, node, portName);
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
                buildTerminationPoint(transaction, tpPath, tpAugmentationBuilder, node, portUpdate);
                UUID interfaceUuid = (UUID)portUpdate.getValue().getInterfacesColumn().getData().toArray()[0];
                if (interfaceUpdatedRows.containsKey(interfaceUuid)) {
                    buildTerminationPoint(tpAugmentationBuilder, interfaceUpdatedRows.get(interfaceUuid));
                    interfaceUpdatedRows.remove(interfaceUuid);
                    interfaceOldRows.remove(interfaceUuid);
                }
                tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
                if (portOldRows.containsKey(portUpdate.getKey()) && !portQosCleared(portUpdate)) {
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
            Optional<InstanceIdentifier<Node>> bridgeIid = getTerminationPointBridge(transaction, node, interfaceName);
            if (bridgeIid.isPresent()) {
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(interfaceName));
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder, interfaceUpdate.getValue());
                tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
                NodeId bridgeId = SouthboundMapper.createManagedNodeId(bridgeIid.get());
                InstanceIdentifier<TerminationPoint> tpPath = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class,new NodeKey(bridgeId))
                        .child(TerminationPoint.class,tpKey);
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        tpPath, tpBuilder.build());
            }
        }

    }

    private void buildTerminationPoint(ReadWriteTransaction transaction,
            InstanceIdentifier<TerminationPoint> tpPath,
            OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder,
            Node node, Entry<UUID, Port> portUpdate) {

        tpAugmentationBuilder
                .setName(portUpdate.getValue().getName());
        tpAugmentationBuilder.setPortUuid(new Uuid(
                portUpdate.getValue().getUuid().toString()));
        updatePort(transaction, node, tpPath, portUpdate, tpAugmentationBuilder);
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

    private Optional<InstanceIdentifier<Node>> getTerminationPointBridge(UUID portUuid) {
        for (UUID bridgeUuid : this.bridgeUpdatedRows.keySet()) {
            if (this.bridgeUpdatedRows.get(bridgeUuid).getPortsColumn().getData().contains(portUuid)) {
                return Optional.of(
                        SouthboundMapper.createInstanceIdentifier(instanceIdentifierCodec, getOvsdbConnectionInstance(),
                                this.bridgeUpdatedRows.get(bridgeUuid)));
            }
        }
        return Optional.absent();
    }

    @SuppressWarnings("unchecked")
    private Optional<InstanceIdentifier<Node>> getTerminationPointBridge(
            final ReadWriteTransaction transaction, Node node, String tpName) {
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        List<ManagedNodeEntry> managedNodes = ovsdbNode.getManagedNodeEntry();
        TpId tpId = new TpId(tpName);
        for (ManagedNodeEntry managedNodeEntry : managedNodes) {
            Node managedNode = readNode(transaction,
                    (InstanceIdentifier<Node>)managedNodeEntry.getBridgeRef().getValue()).get();
            for (TerminationPoint tpEntry : managedNode.getTerminationPoint()) {
                if (tpId.equals(tpEntry.getTpId())) {
                    return Optional.of((InstanceIdentifier<Node>)managedNodeEntry.getBridgeRef().getValue());
                }
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

    private void updatePort(final ReadWriteTransaction transaction, final Node node,
            final InstanceIdentifier<TerminationPoint> tpPath, final Entry<UUID, Port> port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        updateVlan(port.getValue(), ovsdbTerminationPointBuilder);
        updateVlanTrunks(port.getValue(), ovsdbTerminationPointBuilder);
        updateVlanMode(port.getValue(), ovsdbTerminationPointBuilder);
        updateQos(transaction, node, tpPath, port, ovsdbTerminationPointBuilder);
        updatePortExternalIds(port.getValue(), ovsdbTerminationPointBuilder);
        updatePortOtherConfig(port.getValue(), ovsdbTerminationPointBuilder);
    }

    private void updateInterface(final Interface interf,
            final String type,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        ovsdbTerminationPointBuilder.setInterfaceUuid(
                new Uuid(interf.getUuid().toString()));
        ovsdbTerminationPointBuilder.setInterfaceType(
                SouthboundMapper.createInterfaceType(type));
        updateIfIndex(interf, ovsdbTerminationPointBuilder);
        updateOfPort(interf, ovsdbTerminationPointBuilder);
        updateOfPortRequest(interf, ovsdbTerminationPointBuilder);
        updateInterfaceExternalIds(interf, ovsdbTerminationPointBuilder);
        updateOptions(interf, ovsdbTerminationPointBuilder);
        updateInterfaceOtherConfig(interf, ovsdbTerminationPointBuilder);
        updateInterfaceLldp(interf, ovsdbTerminationPointBuilder);
        updateInterfaceBfd(interf, ovsdbTerminationPointBuilder);
        updateInterfaceBfdStatus(interf, ovsdbTerminationPointBuilder);
        updateInterfacePolicing(interf, ovsdbTerminationPointBuilder);
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
        List<Trunks> modelTrunks = new ArrayList<>();
        if (!portTrunks.isEmpty()) {
            for (Long trunk: portTrunks) {
                if (trunk != null) {
                    modelTrunks.add(new TrunksBuilder()
                        .setTrunk(new VlanId(trunk.intValue())).build());
                }
            }
        }
        ovsdbTerminationPointBuilder.setTrunks(modelTrunks);
    }

    private void updateVlanMode(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Collection<String> vlanMode = port.getVlanModeColumn().getData();
        if (!vlanMode.isEmpty()) {
            Iterator<String> itr = vlanMode.iterator();
            String vlanType = itr.next();
            if (vlanType.equals(SouthboundConstants.VlanModes.ACCESS.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access);
            } else if (vlanType.equals(SouthboundConstants.VlanModes.NATIVE_TAGGED.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.NativeTagged);
            } else if (vlanType.equals(SouthboundConstants.VlanModes.NATIVE_UNTAGGED.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.NativeUntagged);
            } else if (vlanType.equals(SouthboundConstants.VlanModes.TRUNK.getMode())) {
                ovsdbTerminationPointBuilder
                    .setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Trunk);
            } else {
                LOG.debug("Invalid vlan mode {}.", vlanType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateQos(final ReadWriteTransaction transaction, final Node node,
                           InstanceIdentifier<TerminationPoint> tpPath, final Entry<UUID, Port> port,
                           final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        if (port.getValue() == null) {
            return;
        }
        Collection<UUID> qosUuidCol = port.getValue().getQosColumn().getData();
        if (!qosUuidCol.isEmpty()) {
            UUID qosUuid = qosUuidCol.iterator().next();
            ovsdbTerminationPointBuilder.setQos(new Uuid(qosUuid.toString()));

            NodeId nodeId = node.getNodeId();
            OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);

            // Delete an older QoS entry
            if (portOldRows.containsKey(port.getKey()) && portOldRows.get(port.getKey()).getQosColumn() != null) {
                Collection<UUID> oldQos = portOldRows.get(port.getKey()).getQosColumn().getData();
                if (!oldQos.isEmpty()) {
                    UUID oldQosUuid = oldQos.iterator().next();
                    if (!oldQosUuid.equals(qosUuid)) {
                        InstanceIdentifier<QosEntries> oldQosIid = getQosIid(nodeId, ovsdbNode, oldQosUuid);
                        if (oldQosIid != null) {
                            InstanceIdentifier<QosEntry> oldPortQosIid = tpPath
                                .augmentation(OvsdbTerminationPointAugmentation.class)
                                .child(QosEntry.class,
                                      new QosEntryKey(new Long(SouthboundConstants.PORT_QOS_LIST_KEY)));
//                                    new QosEntryKey(new OvsdbQosRef(oldQosIid)));
                            transaction.delete(LogicalDatastoreType.OPERATIONAL, oldPortQosIid);
                        }
                    }
                }
            }

            InstanceIdentifier<QosEntries> qosIid = getQosIid(nodeId, ovsdbNode, qosUuid);
            if (qosIid != null) {
                List<QosEntry> qosList = new ArrayList<>();
                OvsdbQosRef qosRef = new OvsdbQosRef(qosIid);
                qosList.add(new QosEntryBuilder()
                    .setKey(new QosEntryKey(new Long(SouthboundConstants.PORT_QOS_LIST_KEY)))
                    .setQosRef(qosRef).build());
                ovsdbTerminationPointBuilder.setQosEntry(qosList);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private InstanceIdentifier<QosEntries> getQosIid(NodeId nodeId, OvsdbNodeAugmentation ovsdbNode, UUID qosUuid) {
        // Search for the QoS entry first in the operational datastore
        for (QosEntries qosEntry : ovsdbNode.getQosEntries()) {
            if (qosEntry.getQosUuid().equals(new Uuid(qosUuid.toString()))) {
                return SouthboundMapper.createInstanceIdentifier(nodeId)
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, new QosEntriesKey(qosEntry.getQosId()));
            }
        }

        // Search for the QoS entry in the current OVS updates
        for (Entry<UUID, Qos> qosUpdate : qosUpdatedRows.entrySet()) {
            Qos qos = qosUpdate.getValue();
            if (qos.getUuid().equals(qosUuid)) {
                if (qos.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
                    return (InstanceIdentifier<QosEntries>) instanceIdentifierCodec.bindingDeserializerOrNull(
                            qos.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY));
                } else {
                    return SouthboundMapper.createInstanceIdentifier(nodeId)
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(QosEntries.class, new QosEntriesKey(
                                    new Uri(SouthboundConstants.QOS_URI_PREFIX + "://" + qosUuid.toString())));
                }
            }
        }
        LOG.debug("QoS UUID {} assigned to port not found in operational node {} or QoS updates", qosUuid, ovsdbNode);
        return SouthboundMapper.createInstanceIdentifier(nodeId)
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, new QosEntriesKey(
                        new Uri(SouthboundConstants.QOS_URI_PREFIX + "://" + qosUuid.toString())));
    }

    private void updateIfIndex(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        Set<Long> ifIndexSet = null;
        try {
            if (interf.getIfIndexColumn() != null) {
                ifIndexSet = interf.getIfIndexColumn().getData();
            }
            if (ifIndexSet != null && !ifIndexSet.isEmpty()) {
                for (Long ifIndex : ifIndexSet) {
                    ovsdbTerminationPointBuilder.setIfindex(ifIndex);
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("ifindex", "Interface", e);
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
                    new ArrayList<>();
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
            List<PortExternalIds> externalIdsList = new ArrayList<>();
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
            List<Options> options = new ArrayList<>();
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
            List<PortOtherConfigs> portOtherConfigs = new ArrayList<>();
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

    private void updateInterfaceLldp(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        try {
            Map<String, String> interfaceLldpMap = interf.getLldpColumn().getData();
            if (interfaceLldpMap != null && !interfaceLldpMap.isEmpty()) {
                List<InterfaceLldp> interfaceLldpList = new ArrayList<>();
                for (String interfaceLldpKeyString : interfaceLldpMap.keySet()) {
                    String interfaceLldpValueString = interfaceLldpMap.get(interfaceLldpKeyString);
                    if (interfaceLldpKeyString != null && interfaceLldpValueString != null) {
                        interfaceLldpList.add(new InterfaceLldpBuilder()
                                .setKey(new InterfaceLldpKey(interfaceLldpKeyString))
                                .setLldpKey(interfaceLldpKeyString)
                                .setLldpValue(interfaceLldpValueString)
                                .build());
                    }
                }
                ovsdbTerminationPointBuilder.setInterfaceLldp(interfaceLldpList);
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("lldp", "Interface", e);
        }
    }

    private void updateInterfaceOtherConfig(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> interfaceOtherConfigMap = interf.getOtherConfigColumn().getData();
        if (interfaceOtherConfigMap != null && !interfaceOtherConfigMap.isEmpty()) {
            List<InterfaceOtherConfigs> interfaceOtherConfigs = new ArrayList<>();
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

    private void updateInterfaceBfdStatus(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        try {
            Map<String, String> interfaceBfdStatusMap = interf.getBfdStatusColumn().getData();
            if (interfaceBfdStatusMap != null && !interfaceBfdStatusMap.isEmpty()) {
                List<InterfaceBfdStatus> interfaceBfdStatusList = new ArrayList<>();
                for (String interfaceBfdStatusKeyString : interfaceBfdStatusMap.keySet()) {
                    String interfaceBfdStatusValueString = interfaceBfdStatusMap.get(interfaceBfdStatusKeyString);
                    if (interfaceBfdStatusKeyString != null && interfaceBfdStatusValueString != null) {
                        interfaceBfdStatusList.add(new InterfaceBfdStatusBuilder()
                                .setKey(new InterfaceBfdStatusKey(interfaceBfdStatusKeyString))
                                .setBfdStatusKey(interfaceBfdStatusKeyString)
                                .setBfdStatusValue(interfaceBfdStatusValueString)
                                .build());
                    }
                }
                ovsdbTerminationPointBuilder.setInterfaceBfdStatus(interfaceBfdStatusList);
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("bfd", "Interface", e);
        }
    }

    private void updateInterfaceBfd(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        try {
            Map<String, String> interfaceBfdMap = interf.getBfdColumn().getData();
            if (interfaceBfdMap != null && !interfaceBfdMap.isEmpty()) {
                List<InterfaceBfd> interfaceBfdList = new ArrayList<>();
                for (String interfaceBfdKeyString : interfaceBfdMap.keySet()) {
                    String interfaceBfdValueString = interfaceBfdMap.get(interfaceBfdKeyString);
                    if (interfaceBfdKeyString != null && interfaceBfdValueString != null) {
                        interfaceBfdList.add(new InterfaceBfdBuilder()
                                .setKey(new InterfaceBfdKey(interfaceBfdKeyString))
                                .setBfdKey(interfaceBfdKeyString)
                                .setBfdValue(interfaceBfdValueString)
                                .build());
                    }
                }
                ovsdbTerminationPointBuilder.setInterfaceBfd(interfaceBfdList);
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("bfd", "Interface", e);

        }
    }

    private void updateInterfacePolicing(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Long ingressPolicingRate = null;
        if (interf.getIngressPolicingRateColumn() != null) {
            ingressPolicingRate = interf.getIngressPolicingRateColumn().getData();
        }
        if (ingressPolicingRate != null) {
            if (ingressPolicingRate >= 0) {
                ovsdbTerminationPointBuilder
                    .setIngressPolicingRate(ingressPolicingRate);
            } else {
                LOG.debug("Received negative value for ingressPolicingRate from ovsdb for {} {}",
                        interf.getName(),ingressPolicingRate);
            }
        }

        Long ingressPolicingBurst = null;
        if (interf.getIngressPolicingBurstColumn() != null) {
            ingressPolicingBurst = interf.getIngressPolicingBurstColumn().getData();
        }
        if (ingressPolicingBurst != null) {
            if (ingressPolicingBurst >= 0) {
                ovsdbTerminationPointBuilder
                    .setIngressPolicingBurst(ingressPolicingBurst);
            } else {
                LOG.debug("Received negative value for ingressPolicingBurst from ovsdb for {} {}",
                        interf.getName(),ingressPolicingBurst);
            }
        }
    }

    private boolean portQosCleared(Entry<UUID, Port> portUpdate) {
        if (portUpdate.getValue().getQosColumn() == null) {
            return false;
        }
        Collection<UUID> newQos = portUpdate.getValue().getQosColumn().getData();
        if (portOldRows.get(portUpdate.getKey()).getQosColumn() == null) {
            return false;
        }
        Collection<UUID> oldQos = portOldRows.get(portUpdate.getKey()).getQosColumn().getData();

        if (newQos.isEmpty() && !oldQos.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private InstanceIdentifier<TerminationPoint> getInstanceIdentifier(InstanceIdentifier<Node> bridgeIid,Port port) {
        if (port.getExternalIdsColumn() != null
                && port.getExternalIdsColumn().getData() != null
                && port.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = port.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            return (InstanceIdentifier<TerminationPoint>) instanceIdentifierCodec.bindingDeserializerOrNull(iidString);
        } else {
            return bridgeIid.child(TerminationPoint.class, new TerminationPointKey(new TpId(port.getName())));
        }
    }
}
