/*
 * Copyright (c) 2014, 2017 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQosRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntryBuilder;
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
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
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
            updateTerminationPoints(transaction, node.orElseThrow());
        }
    }

    @VisibleForTesting
    void updateTerminationPoints(ReadWriteTransaction transaction, Node node) {
        for (Entry<UUID, Port> portUpdate : portUpdatedRows.entrySet()) {
            String portName = null;
            portName = portUpdate.getValue().getNameColumn().getData();
            Optional<InstanceIdentifier<Node>> optBridgeIid = getTerminationPointBridge(portUpdate.getKey());
            if (optBridgeIid.isEmpty()) {
                optBridgeIid = getTerminationPointBridge(transaction, node, portName);
            }
            if (optBridgeIid.isPresent()) {
                InstanceIdentifier<Node> bridgeIid = optBridgeIid.orElseThrow();
                final NodeId bridgeId = SouthboundMapper.createManagedNodeId(bridgeIid);
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
                getOvsdbConnectionInstance().updatePortInterface(portName, bridgeIid);
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.withKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                InstanceIdentifier<TerminationPoint> tpPath =
                        getInstanceIdentifier(bridgeIid, portUpdate.getValue());
                OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                buildTerminationPoint(transaction, tpPath, tpAugmentationBuilder, node, portUpdate);
                UUID interfaceUuid = (UUID)portUpdate.getValue().getInterfacesColumn().getData().toArray()[0];
                if (interfaceUpdatedRows.containsKey(interfaceUuid)) {
                    buildTerminationPoint(tpAugmentationBuilder, interfaceUpdatedRows.get(interfaceUuid));
                    interfaceUpdatedRows.remove(interfaceUuid);
                    interfaceOldRows.remove(interfaceUuid);
                }
                tpBuilder.addAugmentation(tpAugmentationBuilder.build());
                if (portOldRows.containsKey(portUpdate.getKey()) && !portQosCleared(portUpdate)) {
                    updateToDataStore(transaction, tpBuilder, tpPath, true);
                    LOG.info("DEVICE - {} TerminationPoint : {} to Bridge : {}", TransactionType.ADD,
                            tpKey.getTpId().getValue(), bridgeId.getValue());
                } else {
                    updateToDataStore(transaction, tpBuilder, tpPath, false);
                    LOG.debug("DEVICE - {} TerminationPoint : {} to Bridge : {}", TransactionType.UPDATE,
                            tpKey.getTpId().getValue(), bridgeId.getValue());
                }
            }
        }
        for (Entry<UUID, Interface> interfaceUpdate : interfaceUpdatedRows.entrySet()) {
            String interfaceName = null;
            Optional<InstanceIdentifier<Node>> bridgeIid = Optional.empty();
            interfaceName = interfaceUpdatedRows.get(interfaceUpdate.getKey()).getNameColumn().getData();
            if (getOvsdbConnectionInstance().getPortInterface(interfaceName) != null) {
                bridgeIid = Optional.of(getOvsdbConnectionInstance().getPortInterface(interfaceName));
            }
            if (bridgeIid.isEmpty()) {
                bridgeIid = getTerminationPointBridge(transaction, node, interfaceName);
            }
            if (bridgeIid.isPresent()) {
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(interfaceName));
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.withKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder, interfaceUpdate.getValue());
                tpBuilder.addAugmentation(tpAugmentationBuilder.build());
                NodeId bridgeId = SouthboundMapper.createManagedNodeId(bridgeIid.orElseThrow());
                InstanceIdentifier<TerminationPoint> tpPath = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class,new NodeKey(bridgeId))
                        .child(TerminationPoint.class,tpKey);
                updateToDataStore(transaction, tpBuilder, tpPath, true);
            }
        }

    }

    protected void updateToDataStore(ReadWriteTransaction transaction, TerminationPointBuilder tpBuilder,
                                     InstanceIdentifier<TerminationPoint> tpPath, boolean merge) {
        if (merge) {
            transaction.merge(LogicalDatastoreType.OPERATIONAL, tpPath, tpBuilder.build());
        } else {
            transaction.put(LogicalDatastoreType.OPERATIONAL, tpPath, tpBuilder.build());
        }
    }

    @VisibleForTesting
    void buildTerminationPoint(ReadWriteTransaction transaction,
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

    @SuppressWarnings("IllegalCatch")
    private Optional<Node> readNode(final ReadWriteTransaction transaction, final InstanceIdentifier<Node> nodePath) {
        Optional<Node> node = Optional.empty();
        try {
            node = SouthboundUtil.readNode(transaction, nodePath);
        } catch (Exception exp) {
            LOG.error("Error in getting the Node for {}", nodePath, exp);
        }
        return node;
    }

    private Optional<InstanceIdentifier<Node>> getTerminationPointBridge(UUID portUuid) {

        if (bridgeUpdatedRows != null) {
            for (Entry<UUID, Bridge> entry : this.bridgeUpdatedRows.entrySet()) {
                UUID bridgeUuid = entry.getKey();
                if (this.bridgeUpdatedRows.get(bridgeUuid).getPortsColumn().getData()
                    .contains(portUuid)) {
                    InstanceIdentifier<Node> iid = SouthboundMapper.createInstanceIdentifier(
                        instanceIdentifierCodec, getOvsdbConnectionInstance(),
                        this.bridgeUpdatedRows.get(bridgeUuid));
                    getOvsdbConnectionInstance().updatePort(portUuid, iid);
                    return Optional.of(iid);
                }
            }
        }
        if (getOvsdbConnectionInstance().getPort(portUuid) != null) {
            return Optional.of(getOvsdbConnectionInstance().getPort(portUuid));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    // FIXME: non-static for implementation internals mocking
    private Optional<InstanceIdentifier<Node>> getTerminationPointBridge(
            final ReadWriteTransaction transaction, Node node, String tpName) {
        OvsdbNodeAugmentation ovsdbNode = node.augmentation(OvsdbNodeAugmentation.class);
        Map<ManagedNodeEntryKey, ManagedNodeEntry> managedNodes = ovsdbNode.nonnullManagedNodeEntry();
        TpId tpId = new TpId(tpName);

        for (ManagedNodeEntry managedNodeEntry : managedNodes.values()) {
            Optional<Node> optManagedNode = SouthboundUtil.readNode(transaction,
                    (InstanceIdentifier<Node>)managedNodeEntry.getBridgeRef().getValue());
            if (optManagedNode.isPresent()) {
                Node managedNode = optManagedNode.orElseThrow();
                Map<TerminationPointKey, TerminationPoint> tpEntrys = managedNode.getTerminationPoint();
                if (tpEntrys != null) {
                    TerminationPoint tpEntry = tpEntrys.get(new TerminationPointKey(tpId));
                    if (tpEntry != null) {
                        return Optional.of((InstanceIdentifier<Node>) managedNodeEntry.getBridgeRef().getValue());
                    }
                }
            }
        }

        return Optional.empty();
    }

    @VisibleForTesting
    void updateInterfaces(Interface interfaceUpdate,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Column<GenericTableSchema, String> typeColumn = interfaceUpdate.getTypeColumn();
        String type = typeColumn.getData();
        updateInterface(interfaceUpdate, type,ovsdbTerminationPointBuilder);
    }

    @VisibleForTesting
    void updatePort(final ReadWriteTransaction transaction, final Node node,
            final InstanceIdentifier<TerminationPoint> tpPath, final Entry<UUID, Port> port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        updateVlan(port.getValue(), ovsdbTerminationPointBuilder);
        updateVlanTrunks(port.getValue(), ovsdbTerminationPointBuilder);
        updateVlanMode(port.getValue(), ovsdbTerminationPointBuilder);
        updateQos(transaction, node, tpPath, port, ovsdbTerminationPointBuilder);
        updatePortExternalIds(port.getValue(), ovsdbTerminationPointBuilder);
        updatePortOtherConfig(port.getValue(), ovsdbTerminationPointBuilder);
    }

    @VisibleForTesting
    void updateInterface(final Interface interf,
            final String type,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        ovsdbTerminationPointBuilder.setInterfaceUuid(
                new Uuid(interf.getUuid().toString()));
        ovsdbTerminationPointBuilder.setInterfaceType(
                SouthboundMapper.createInterfaceType(type));
        updateIfIndex(interf, ovsdbTerminationPointBuilder);
        updateMac(interf, ovsdbTerminationPointBuilder);
        updateMacInUse(interf, ovsdbTerminationPointBuilder);
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

    @VisibleForTesting
    void updateVlan(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Collection<Long> vlanId = port.getTagColumn().getData();
        if (vlanId.size() > 0) {
            Iterator<Long> itr = vlanId.iterator();
            // There are no loops here, just get the first element.
            int id = itr.next().intValue();
            ovsdbTerminationPointBuilder.setVlanTag(new VlanId(Uint16.valueOf(id)));
        }
    }

    @VisibleForTesting
    void updateVlanTrunks(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Set<Long> portTrunks = port.getTrunksColumn().getData();
        List<Trunks> modelTrunks = new ArrayList<>();
        if (!portTrunks.isEmpty()) {
            for (Long trunk: portTrunks) {
                if (trunk != null) {
                    modelTrunks.add(new TrunksBuilder()
                        .setTrunk(new VlanId(Uint16.valueOf(trunk.intValue()))).build());
                }
            }
        }
        ovsdbTerminationPointBuilder.setTrunks(modelTrunks);
    }

    @VisibleForTesting
    void updateVlanMode(final Port port,
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

    private void updateQos(final ReadWriteTransaction transaction, final Node node,
                           InstanceIdentifier<TerminationPoint> tpPath, final Entry<UUID, Port> port,
                           final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        if (port.getValue() == null) {
            return;
        }
        Collection<UUID> qosUuidCol = port.getValue().getQosColumn().getData();
        if (!qosUuidCol.isEmpty()) {
            UUID qosUuid = qosUuidCol.iterator().next();

            NodeId nodeId = node.getNodeId();
            OvsdbNodeAugmentation ovsdbNode = node.augmentation(OvsdbNodeAugmentation.class);

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
                                .child(QosEntry.class, SouthboundConstants.PORT_QOS_LIST_KEY);
                            transaction.delete(LogicalDatastoreType.OPERATIONAL, oldPortQosIid);
                        }
                    }
                }
            }

            InstanceIdentifier<QosEntries> qosIid = getQosIid(nodeId, ovsdbNode, qosUuid);
            if (qosIid != null) {
                ovsdbTerminationPointBuilder.setQosEntry(
                    Map.of(SouthboundConstants.PORT_QOS_LIST_KEY, new QosEntryBuilder()
                        .withKey(SouthboundConstants.PORT_QOS_LIST_KEY)
                        .setQosRef(new OvsdbQosRef(qosIid.toIdentifier()))
                        .build()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private InstanceIdentifier<QosEntries> getQosIid(NodeId nodeId, OvsdbNodeAugmentation ovsdbNode, UUID qosUuid) {
        // Search for the QoS entry first in the operational datastore
        final Uuid uuid = new Uuid(qosUuid.toString());
        for (QosEntries qosEntry : ovsdbNode.nonnullQosEntries().values()) {
            if (uuid.equals(qosEntry.getQosUuid())) {
                return SouthboundMapper.createInstanceIdentifier(nodeId)
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, qosEntry.key());
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

    private static void updateIfIndex(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        Set<Long> ifIndexSet = null;
        try {
            if (interf.getIfIndexColumn() != null) {
                ifIndexSet = interf.getIfIndexColumn().getData();
            }
            if (ifIndexSet != null && !ifIndexSet.isEmpty()) {
                for (Long ifIndex : ifIndexSet) {
                    ovsdbTerminationPointBuilder.setIfindex(Uint32.valueOf(ifIndex));
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("ifindex", "Interface", e);
        }
    }

    private static void updateMac(final Interface interf,
                                  final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        Set<String> macSet = null;
        try {
            if (interf.getMacColumn() != null) {
                macSet = interf.getMacColumn().getData();
            }
            if (macSet != null && !macSet.isEmpty()) {
                /*
                 * It is a set due to way JSON decoder converts [] objects. OVS
                 * only supports ONE mac, so we're fine.
                 */
                for (String mac: macSet) {
                    ovsdbTerminationPointBuilder.setMac(new MacAddress(mac));
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("mac", "Interface", e);
        }
    }

    private static void updateMacInUse(final Interface interf,
                                       final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {
        Set<String> macInUseSet = null;
        try {
            if (interf.getMacInUseColumn() != null) {
                macInUseSet = interf.getMacInUseColumn().getData();
            }
            if (macInUseSet != null && !macInUseSet.isEmpty()) {
                /*
                 * It is a set due to way JSON decoder converts [] objects. OVS
                 * only supports ONE mac, so we're fine.
                 */
                for (String macInUse: macInUseSet) {
                    ovsdbTerminationPointBuilder.setMacInUse(new MacAddress(macInUse));
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("mac_in_use", "Interface", e);
        }
    }

    @VisibleForTesting
    void updateOfPort(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Set<Long> ofPorts = interf.getOpenFlowPortColumn().getData();
        if (ofPorts != null && !ofPorts.isEmpty()) {
            Iterator<Long> ofPortsIter = ofPorts.iterator();
            long ofPort = ofPortsIter.next();
            if (ofPort >= 0) {
                ovsdbTerminationPointBuilder.setOfport(Uint32.valueOf(ofPort));
            } else {
                LOG.debug("Received negative value for ofPort from ovsdb for {} {}", interf.getName(),ofPort);
            }
        }
    }

    @VisibleForTesting
    void updateOfPortRequest(final Interface interf,
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
                ovsdbTerminationPointBuilder.setOfportRequest(Uint16.valueOf(ofPort));
            } else {
                LOG.debug("Received negative value for ofPort from ovsdb for {} {}", interf.getName(),ofPort);
            }
        }
    }

    @VisibleForTesting
    void updateInterfaceExternalIds(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> interfaceExternalIds =
                interf.getExternalIdsColumn().getData();
        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            var externalIdsList = BindingMap.<InterfaceExternalIdsKey, InterfaceExternalIds>orderedBuilder();
            for (Entry<String, String> entry : interfaceExternalIds.entrySet()) {
                String externalIdKey = entry.getKey();
                String externalIdValue = entry.getValue();
                if (externalIdKey != null && externalIdValue != null) {
                    externalIdsList.add(new InterfaceExternalIdsBuilder()
                            .setExternalIdKey(externalIdKey)
                            .setExternalIdValue(externalIdValue)
                            .build());
                }
            }
            ovsdbTerminationPointBuilder.setInterfaceExternalIds(externalIdsList.build());
        }
    }

    @VisibleForTesting
    void updatePortExternalIds(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> portExternalIds = port.getExternalIdsColumn().getData();
        if (portExternalIds != null && !portExternalIds.isEmpty()) {
            var externalIdsList = BindingMap.<PortExternalIdsKey, PortExternalIds>orderedBuilder();
            for (Entry<String, String> entry : portExternalIds.entrySet()) {
                String externalIdKey = entry.getKey();
                String externalIdValue = entry.getValue();
                if (externalIdKey != null && externalIdValue != null) {
                    externalIdsList.add(new PortExternalIdsBuilder()
                            .setExternalIdKey(externalIdKey)
                            .setExternalIdValue(externalIdValue).build());
                }
            }
            ovsdbTerminationPointBuilder.setPortExternalIds(externalIdsList.build());
        }
    }

    @VisibleForTesting
    void updateOptions(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> optionsMap = interf.getOptionsColumn().getData();
        if (optionsMap != null && !optionsMap.isEmpty()) {
            var options = BindingMap.<OptionsKey, Options>orderedBuilder();
            for (Entry<String, String> entry : optionsMap.entrySet()) {
                String optionsKeyString = entry.getKey();
                String optionsValueString = entry.getValue();
                if (optionsKeyString != null && optionsValueString != null) {
                    OptionsKey optionsKey = new OptionsKey(optionsKeyString);
                    options.add(new OptionsBuilder()
                        .withKey(optionsKey)
                        .setValue(optionsValueString).build());
                }
            }
            ovsdbTerminationPointBuilder.setOptions(options.build());
        }
    }

    @VisibleForTesting
    void updatePortOtherConfig(final Port port,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> portOtherConfigMap = port.getOtherConfigColumn().getData();
        if (portOtherConfigMap != null && !portOtherConfigMap.isEmpty()) {
            var portOtherConfigs = BindingMap.<PortOtherConfigsKey, PortOtherConfigs>orderedBuilder();
            for (Entry<String, String> entry : portOtherConfigMap.entrySet()) {
                String portOtherConfigKeyString = entry.getKey();
                String portOtherConfigValueString = entry.getValue();
                if (portOtherConfigKeyString != null && portOtherConfigValueString != null) {
                    portOtherConfigs.add(new PortOtherConfigsBuilder()
                        .setOtherConfigKey(portOtherConfigKeyString)
                        .setOtherConfigValue(portOtherConfigValueString).build());
                }
            }
            ovsdbTerminationPointBuilder.setPortOtherConfigs(portOtherConfigs.build());
        }
    }

    private static void updateInterfaceLldp(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        try {
            Map<String, String> interfaceLldpMap = interf.getLldpColumn().getData();
            if (interfaceLldpMap != null && !interfaceLldpMap.isEmpty()) {
                var interfaceLldpList = BindingMap.<InterfaceLldpKey, InterfaceLldp>orderedBuilder();
                for (Entry<String, String> entry : interfaceLldpMap.entrySet()) {
                    String interfaceLldpKeyString = entry.getKey();
                    String interfaceLldpValueString = entry.getValue();
                    if (interfaceLldpKeyString != null && interfaceLldpValueString != null) {
                        interfaceLldpList.add(new InterfaceLldpBuilder()
                                .withKey(new InterfaceLldpKey(interfaceLldpKeyString))
                                .setLldpKey(interfaceLldpKeyString)
                                .setLldpValue(interfaceLldpValueString)
                                .build());
                    }
                }
                ovsdbTerminationPointBuilder.setInterfaceLldp(interfaceLldpList.build());
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("lldp", "Interface", e);
        }
    }

    @VisibleForTesting
    void updateInterfaceOtherConfig(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Map<String, String> interfaceOtherConfigMap = interf.getOtherConfigColumn().getData();
        if (interfaceOtherConfigMap != null && !interfaceOtherConfigMap.isEmpty()) {
            var interfaceOtherConfigs = BindingMap.<InterfaceOtherConfigsKey, InterfaceOtherConfigs>orderedBuilder();
            for (Entry<String, String> entry : interfaceOtherConfigMap.entrySet()) {
                String interfaceOtherConfigKeyString = entry.getKey();
                String interfaceOtherConfigValueString = entry.getValue();
                if (interfaceOtherConfigKeyString != null && interfaceOtherConfigValueString != null) {
                    interfaceOtherConfigs.add(new InterfaceOtherConfigsBuilder()
                        .setOtherConfigKey(interfaceOtherConfigKeyString)
                        .setOtherConfigValue(interfaceOtherConfigValueString).build());
                }
            }
            ovsdbTerminationPointBuilder.setInterfaceOtherConfigs(interfaceOtherConfigs.build());
        }
    }

    private static void updateInterfaceBfdStatus(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        try {
            Map<String, String> interfaceBfdStatusMap = interf.getBfdStatusColumn().getData();
            if (interfaceBfdStatusMap != null && !interfaceBfdStatusMap.isEmpty()) {
                var interfaceBfdStatusList = BindingMap.<InterfaceBfdStatusKey, InterfaceBfdStatus>orderedBuilder();
                for (Entry<String, String> entry : interfaceBfdStatusMap.entrySet()) {
                    String interfaceBfdStatusKeyString = entry.getKey();
                    String interfaceBfdStatusValueString = entry.getValue();
                    if (interfaceBfdStatusKeyString != null && interfaceBfdStatusValueString != null) {
                        interfaceBfdStatusList.add(new InterfaceBfdStatusBuilder()
                                .withKey(new InterfaceBfdStatusKey(interfaceBfdStatusKeyString))
                                .setBfdStatusKey(interfaceBfdStatusKeyString)
                                .setBfdStatusValue(interfaceBfdStatusValueString)
                                .build());
                    }
                }
                ovsdbTerminationPointBuilder.setInterfaceBfdStatus(interfaceBfdStatusList.build());
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("bfd", "Interface", e);
        }
    }

    private static void updateInterfaceBfd(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        try {
            Map<String, String> interfaceBfdMap = interf.getBfdColumn().getData();
            if (interfaceBfdMap != null && !interfaceBfdMap.isEmpty()) {
                var interfaceBfdList = BindingMap.<InterfaceBfdKey, InterfaceBfd>orderedBuilder();
                for (Entry<String, String> entry : interfaceBfdMap.entrySet()) {
                    String interfaceBfdKeyString = entry.getKey();
                    String interfaceBfdValueString = entry.getValue();
                    if (interfaceBfdKeyString != null && interfaceBfdValueString != null) {
                        interfaceBfdList.add(new InterfaceBfdBuilder()
                                .withKey(new InterfaceBfdKey(interfaceBfdKeyString))
                                .setBfdKey(interfaceBfdKeyString)
                                .setBfdValue(interfaceBfdValueString)
                                .build());
                    }
                }
                ovsdbTerminationPointBuilder.setInterfaceBfd(interfaceBfdList.build());
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("bfd", "Interface", e);

        }
    }

    private static void updateInterfacePolicing(final Interface interf,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder) {

        Long ingressPolicingRate = null;
        if (interf.getIngressPolicingRateColumn() != null) {
            ingressPolicingRate = interf.getIngressPolicingRateColumn().getData();
        }
        if (ingressPolicingRate != null) {
            if (ingressPolicingRate >= 0) {
                ovsdbTerminationPointBuilder
                    .setIngressPolicingRate(Uint32.valueOf(ingressPolicingRate));
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
                    .setIngressPolicingBurst(Uint32.valueOf(ingressPolicingBurst));
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
    @VisibleForTesting
    InstanceIdentifier<TerminationPoint> getInstanceIdentifier(InstanceIdentifier<Node> bridgeIid,Port port) {
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
