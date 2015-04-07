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
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.external.ids.attributes.ExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.external.ids.attributes.ExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
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
                        Collection<Long> vlanId = port.getTagColumn().getData();
                        Set<Long> portTrunks = port.getTrunksColumn().getData();
                        bridgeName = bridge.getName();
                        NodeId bridgeId = SouthboundMapper.createManagedNodeId(
                                getKey(), new OvsdbBridgeName(bridgeName));
                        final InstanceIdentifier<Node> nodePath = SouthboundMapper
                                .createInstanceIdentifier(bridgeId);
                        Optional<Node> node = Optional.absent();
                        try {
                            node = transaction.read(
                                    LogicalDatastoreType.OPERATIONAL, nodePath)
                                    .checkedGet();
                        } catch (final ReadFailedException e) {
                            LOG.warn("Read Operational/DS for Node fail! {}",
                                    nodePath, e);
                        }
                        if (node.isPresent()) {
                            NodeBuilder nodeBuilder = new NodeBuilder();
                            nodeBuilder.setNodeId(bridgeId);

                            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder
                                = new OvsdbTerminationPointAugmentationBuilder();
                            List<TerminationPoint> tpList = new ArrayList<TerminationPoint>();
                            TerminationPointBuilder entry = new TerminationPointBuilder();
                            entry.setTpId(new TpId(port.getName()));
                            ovsdbTerminationPointBuilder
                                    .setName(port.getName());
                            ovsdbTerminationPointBuilder.setPortUuid(new Uuid(
                                    port.getUuid().toString()));
                            if (vlanId.size() > 0) {
                                Iterator<Long> itr = vlanId.iterator();
                                // There are no loops here, just get the first element.
                                int id = itr.next().intValue();
                                ovsdbTerminationPointBuilder.setVlanTag(new VlanId(id));
                            }
                            List<Trunks> modelTrunks = new ArrayList<Trunks>();
                            for (Long trunk: portTrunks) {
                                if (trunk != null) {
                                    modelTrunks.add(new TrunksBuilder().setTrunk(new VlanId(trunk.intValue())).build());
                                }
                            }
                            ovsdbTerminationPointBuilder.setTrunks(modelTrunks);
                            Column<GenericTableSchema, Set<UUID>> iface = port.getInterfacesColumn();
                            Set<UUID> ifUuid = iface.getData();
                            Collection<Interface> ifUpdateRows = TyperUtils.extractRowsUpdated(
                                    Interface.class, getUpdates(),  getDbSchema()).values();
                            for (UUID ifIter : ifUuid) {
                                for (Interface interfIter : ifUpdateRows) {
                                    Column<GenericTableSchema, String> typeColumn = interfIter.getTypeColumn();
                                    String type = typeColumn.getData();
                                    if ((interfIter.getUuid()).equals(ifIter)) {
                                        ovsdbTerminationPointBuilder.setInterfaceUuid(
                                                new Uuid(interfIter.getUuid().toString()));
                                        ovsdbTerminationPointBuilder.setInterfaceType(
                                                SouthboundMapper.createInterfaceType(type));
                                        Set<Long> ofPorts = interfIter.getOpenFlowPortColumn().getData();
                                        if (ofPorts != null && !ofPorts.isEmpty()) {
                                            Iterator<Long> ofPortsIter = ofPorts.iterator();
                                            long ofPort = ofPortsIter.next();
                                            if (ofPort >= 0) {
                                                ovsdbTerminationPointBuilder
                                                    .setOfport(ofPort);
                                            } else {
                                                LOG.debug("Received negative value for ofPort from ovsdb for {} {} {}",
                                                        bridge.getName(), interfIter.getName(),ofPort);
                                            }
                                        }
                                        Set<Long> ofPortRequests = interfIter
                                                .getOpenFlowPortRequestColumn().getData();
                                        if (ofPortRequests != null && !ofPortRequests.isEmpty()) {
                                            Iterator<Long> ofPortRequestsIter = ofPortRequests.iterator();
                                            int ofPort = ofPortRequestsIter.next().intValue();
                                            if (ofPort >= 0) {
                                                ovsdbTerminationPointBuilder
                                                    .setOfportRequest(ofPort);
                                            } else {
                                                LOG.debug("Received negative value for ofPort from ovsdb for {} {} {}",
                                                        bridge.getName(), interfIter.getName(),ofPort);
                                            }
                                        }

                                        Map<String, String> externalIds = interfIter.getExternalIdsColumn().getData();
                                        if (externalIds != null && !externalIds.isEmpty()) {
                                            Set<String> externalIdKeys = externalIds.keySet();
                                            ArrayList<ExternalIds> externalIdsList = new ArrayList<ExternalIds>();
                                            String externalIdValue;
                                            for (String externalIdKey : externalIdKeys) {
                                                externalIdValue = externalIds.get(externalIdKey);
                                                if (externalIdKey != null && externalIdValue != null) {
                                                    externalIdsList.add(new ExternalIdsBuilder()
                                                            .setExternalIdKey(externalIdKey)
                                                            .setExternalIdValue(externalIdValue).build());
                                                }
                                            }
                                            ovsdbTerminationPointBuilder.setExternalIds(externalIdsList);
                                        }

                                        Map<String, String> optionsMap = interfIter.getOptionsColumn().getData();
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

                                        break;
                                    }
                                }
                            }
                            entry.addAugmentation(
                                    OvsdbTerminationPointAugmentation.class,
                                    ovsdbTerminationPointBuilder.build());

                            tpList.add(entry.build());
                            nodeBuilder.setTerminationPoint(tpList);
                            nodeBuilder.addAugmentation(
                                    OvsdbBridgeAugmentation.class,
                                    node.get().getAugmentation(
                                            OvsdbBridgeAugmentation.class));
                            transaction.merge(LogicalDatastoreType.OPERATIONAL,
                                    nodePath, nodeBuilder.build());
                        }
                    }
                }
            }
        }
    }
}
