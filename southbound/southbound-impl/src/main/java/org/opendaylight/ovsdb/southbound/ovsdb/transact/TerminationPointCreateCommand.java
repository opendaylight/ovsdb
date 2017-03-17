/*
 * Copyright (c) 2015, 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationPointCreateCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointCreateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreated(events, OvsdbTerminationPointAugmentation.class),
                TransactUtils.extractCreatedOrUpdated(events, Node.class), instanceIdentifierCodec);
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreated(modifications, OvsdbTerminationPointAugmentation.class),
                TransactUtils.extractCreatedOrUpdated(modifications, Node.class), instanceIdentifierCodec);
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                    createdTerminationPoints,
            Map<InstanceIdentifier<Node>, Node> nodes, InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> entry :
                createdTerminationPoints.entrySet()) {
            OvsdbTerminationPointAugmentation terminationPoint = entry.getValue();
            LOG.debug("Received request to create termination point {}",
                    terminationPoint.getName());
            InstanceIdentifier terminationPointIid = entry.getKey();
            Optional<TerminationPoint> terminationPointOptional =
                    state.getBridgeTerminationPoint(terminationPointIid);
            if (!terminationPointOptional.isPresent()) {
                // Configure interface
                String interfaceUuid = "Interface_" + SouthboundMapper.getRandomUuid();
                Interface ovsInterface =
                        TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Interface.class);
                createInterface(terminationPoint, ovsInterface);
                transaction.add(op.insert(ovsInterface).withId(interfaceUuid));

                stampInstanceIdentifier(transaction, entry.getKey(), ovsInterface.getName(), instanceIdentifierCodec);

                // Configure port with the above interface details
                String portUuid = "Port_" + SouthboundMapper.getRandomUuid();
                Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
                createPort(terminationPoint, port, interfaceUuid);
                transaction.add(op.insert(port).withId(portUuid));
                LOG.info("Created Termination Point : {} with Uuid : {}",
                        terminationPoint.getName(),
                        terminationPoint.getPortUuid());
                //Configure bridge with the above port details
                Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                if (getBridge(entry.getKey(), nodes) != null) {
                    bridge.setName(getBridge(entry.getKey(), nodes).getBridgeName().getValue());
                    bridge.setPorts(Collections.singleton(new UUID(portUuid)));

                    transaction.add(op.mutate(bridge)
                            .addMutation(bridge.getPortsColumn().getSchema(),
                                    Mutator.INSERT, bridge.getPortsColumn().getData())
                            .where(bridge.getNameColumn().getSchema()
                                    .opEqual(bridge.getNameColumn().getData())).build());
                }
            }
        }

    }

    private void createInterface(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {
        ovsInterface.setName(terminationPoint.getName());

        createInterfaceType(terminationPoint, ovsInterface);
        createOfPort(terminationPoint, ovsInterface);
        createOfPortRequest(terminationPoint, ovsInterface);
        createInterfaceOptions(terminationPoint, ovsInterface);
        createInterfaceOtherConfig(terminationPoint, ovsInterface);
        createInterfaceExternalIds(terminationPoint, ovsInterface);
        createInterfaceLldp(terminationPoint, ovsInterface);
        createInterfaceBfd(terminationPoint, ovsInterface);
    }

    private void createInterfaceType(final OvsdbTerminationPointAugmentation terminationPoint,
                                     final Interface ovsInterface) {

        Class<? extends InterfaceTypeBase> mdsaltype = terminationPoint.getInterfaceType();
        if (mdsaltype != null) {
            ovsInterface.setType(SouthboundMapper.createOvsdbInterfaceType(mdsaltype));
        }
    }

    private void createPort(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port, final String interfaceUuid) {

        port.setName(terminationPoint.getName());
        port.setInterfaces(Collections.singleton(new UUID(interfaceUuid)));
        createPortOtherConfig(terminationPoint, port);
        createPortVlanTag(terminationPoint, port);
        createPortVlanTrunk(terminationPoint, port);
        createPortVlanMode(terminationPoint, port);
        createPortExternalIds(terminationPoint, port);
    }

    private void createOfPort(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Long ofPort = terminationPoint.getOfport();
        if (ofPort != null) {
            ovsInterface.setOpenFlowPort(Collections.singleton(ofPort));
        }
    }

    private void createOfPortRequest(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Integer ofPortRequest = terminationPoint.getOfportRequest();
        if (ofPortRequest != null) {
            ovsInterface.setOpenFlowPortRequest(Collections.singleton(ofPortRequest.longValue()));
        }
    }

    private void createInterfaceOptions(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        //Configure optional input
        if (terminationPoint.getOptions() != null) {
            try {
                ovsInterface.setOptions(YangUtils.convertYangKeyValueListToMap(terminationPoint.getOptions(),
                        Options::getOption, Options::getValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface options", e);
            }
        }
    }

    private void createInterfaceExternalIds(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        List<InterfaceExternalIds> interfaceExternalIds =
                terminationPoint.getInterfaceExternalIds();
        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            try {
                ovsInterface.setExternalIds(YangUtils.convertYangKeyValueListToMap(interfaceExternalIds,
                        InterfaceExternalIds::getExternalIdKey, InterfaceExternalIds::getExternalIdValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface external_ids", e);
            }
        }
    }

    private void createInterfaceOtherConfig(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        List<InterfaceOtherConfigs> interfaceOtherConfigs =
                terminationPoint.getInterfaceOtherConfigs();
        if (interfaceOtherConfigs != null && !interfaceOtherConfigs.isEmpty()) {
            Map<String, String> otherConfigsMap = new HashMap<>();
            for (InterfaceOtherConfigs interfaceOtherConfig : interfaceOtherConfigs) {
                otherConfigsMap.put(interfaceOtherConfig.getOtherConfigKey(),
                        interfaceOtherConfig.getOtherConfigValue());
            }
            try {
                ovsInterface.setOtherConfig(otherConfigsMap);
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface other_config", e);
            }
        }
    }

    private void createInterfaceLldp(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        try {
            List<InterfaceLldp> interfaceLldpList =
                    terminationPoint.getInterfaceLldp();
            if (interfaceLldpList != null && !interfaceLldpList.isEmpty()) {
                try {
                    ovsInterface.setLldp(YangUtils.convertYangKeyValueListToMap(interfaceLldpList,
                            InterfaceLldp::getLldpKey, InterfaceLldp::getLldpValue));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB interface lldp", e);
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("lldp", "Interface", e);
        }
    }

    private void createInterfaceBfd(final OvsdbTerminationPointAugmentation terminationPoint,
                    final Interface ovsInterface) {

        try {
            List<InterfaceBfd> interfaceBfdList = terminationPoint.getInterfaceBfd();
            if (interfaceBfdList != null && !interfaceBfdList.isEmpty()) {
                try {
                    ovsInterface.setBfd(YangUtils.convertYangKeyValueListToMap(interfaceBfdList,
                                    InterfaceBfd::getBfdKey, InterfaceBfd::getBfdValue));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB interface bfd", e);
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("bfd", "Interface", e);
        }
    }

    private void createPortExternalIds(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        List<PortExternalIds> portExternalIds = terminationPoint.getPortExternalIds();
        if (portExternalIds != null && !portExternalIds.isEmpty()) {
            try {
                port.setExternalIds(YangUtils.convertYangKeyValueListToMap(portExternalIds,
                        PortExternalIds::getExternalIdKey, PortExternalIds::getExternalIdValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB port external_ids", e);
            }
        }
    }

    private void createPortVlanTag(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        if (terminationPoint.getVlanTag() != null) {
            Set<Long> vlanTag = new HashSet<>();
            vlanTag.add(terminationPoint.getVlanTag().getValue().longValue());
            port.setTag(vlanTag);
        }
    }

    private void createPortVlanTrunk(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        if (terminationPoint.getTrunks() != null && terminationPoint.getTrunks().size() > 0) {
            Set<Long> portTrunks = new HashSet<>();
            List<Trunks> modelTrunks = terminationPoint.getTrunks();
            for (Trunks trunk: modelTrunks) {
                if (trunk.getTrunk() != null) {
                    portTrunks.add(trunk.getTrunk().getValue().longValue());
                }
            }
            port.setTrunks(portTrunks);
        }
    }

    private void createPortVlanMode(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {
        if (terminationPoint.getVlanMode() != null) {
            Set<String> portVlanMode = new HashSet<>();
            VlanMode modelVlanMode = terminationPoint.getVlanMode();
            portVlanMode.add(SouthboundConstants.VlanModes.values()[modelVlanMode.getIntValue() - 1].getMode());
            port.setVlanMode(portVlanMode);
        }
    }

    private void createPortOtherConfig(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port ovsPort) {
        List<PortOtherConfigs> portOtherConfigs =
                terminationPoint.getPortOtherConfigs();
        if (portOtherConfigs != null && !portOtherConfigs.isEmpty()) {
            try {
                ovsPort.setOtherConfig(YangUtils.convertYangKeyValueListToMap(portOtherConfigs,
                        PortOtherConfigs::getOtherConfigKey, PortOtherConfigs::getOtherConfigValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB port other_config", e);
            }
        }
    }

    private OvsdbBridgeAugmentation getBridge(InstanceIdentifier<?> key, Map<InstanceIdentifier<Node>, Node> nodes) {
        OvsdbBridgeAugmentation bridge = null;
        InstanceIdentifier<Node> nodeIid = key.firstIdentifierOf(Node.class);
        if (nodes != null && nodes.get(nodeIid) != null) {
            Node node = nodes.get(nodeIid);
            bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
            if (bridge == null) {
                ReadOnlyTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction();
                CheckedFuture<Optional<Node>, ReadFailedException> future =
                        transaction.read(LogicalDatastoreType.OPERATIONAL, nodeIid);
                try {
                    Optional<Node> nodeOptional = future.get();
                    if (nodeOptional.isPresent()) {
                        bridge = nodeOptional.get().getAugmentation(OvsdbBridgeAugmentation.class);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Error reading from datastore",e);
                }
                transaction.close();
            }
        }
        return bridge;
    }

    public static void stampInstanceIdentifier(TransactionBuilder transaction,
            InstanceIdentifier<OvsdbTerminationPointAugmentation> iid, String interfaceName,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
        port.setName(interfaceName);
        port.setExternalIds(Collections.emptyMap());
        Mutate mutate = TransactUtils.stampInstanceIdentifierMutation(transaction, iid, port.getSchema(),
                port.getExternalIdsColumn().getSchema(), instanceIdentifierCodec);
        transaction.add(mutate
                .where(port.getNameColumn().getSchema().opEqual(interfaceName))
                .build());
    }

}
