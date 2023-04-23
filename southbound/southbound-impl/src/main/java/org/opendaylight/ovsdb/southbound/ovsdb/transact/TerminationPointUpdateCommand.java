/*
 * Copyright © 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQosRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceLldpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationPointUpdateCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreatedOrUpdated(events, OvsdbTerminationPointAugmentation.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbTerminationPointAugmentation.class),
                instanceIdentifierCodec);
    }

    private void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                    createdOrUpdated,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<OvsdbTerminationPointAugmentation>,
                 OvsdbTerminationPointAugmentation> terminationPointEntry : createdOrUpdated.entrySet()) {
            updateTerminationPoint(transaction, state, terminationPointEntry.getKey(),
                    terminationPointEntry.getValue(), instanceIdentifierCodec);
        }
    }

    public void updateTerminationPoint(final TransactionBuilder transaction, final BridgeOperationalState state,
            final InstanceIdentifier<OvsdbTerminationPointAugmentation> iid,
            final OvsdbTerminationPointAugmentation terminationPoint,
            final InstanceIdentifierCodec instanceIdentifierCodec) {

        if (terminationPoint != null) {
            LOG.debug("Received request to update termination point {}",
                   terminationPoint.getName());

            // Update interface
            Interface ovsInterface = transaction.getTypedRowWrapper(Interface.class);
            updateInterface(terminationPoint, ovsInterface);
            Interface extraInterface = transaction.getTypedRowWrapper(Interface.class);
            extraInterface.setName("");
            transaction.add(op.update(ovsInterface)
                    .where(extraInterface.getNameColumn().getSchema().opEqual(terminationPoint.getName()))
                    .build());

            TerminationPointCreateCommand.stampInstanceIdentifier(transaction,
                    iid.firstIdentifierOf(OvsdbTerminationPointAugmentation.class), terminationPoint.getName(),
                    instanceIdentifierCodec);
            final String opendaylightIid = instanceIdentifierCodec.serialize(iid);
            // Update port
            // Bug#6136
            Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional = state.getOvsdbBridgeAugmentation(iid);
            if (ovsdbBridgeOptional != null && ovsdbBridgeOptional.isPresent()) {
                OvsdbBridgeAugmentation operBridge = ovsdbBridgeOptional.orElseThrow();
                if (operBridge != null) {
                    Port port = transaction.getTypedRowWrapper(Port.class);
                    updatePort(terminationPoint, port, operBridge, opendaylightIid);
                    Port extraPort = transaction.getTypedRowWrapper(Port.class);
                    extraPort.setName("");
                    transaction.add(op.update(port)
                        .where(extraPort.getNameColumn().getSchema().opEqual(terminationPoint.getName()))
                        .build());
                    LOG.debug("Updated Termination Point : {}  with Uuid : {}",
                        terminationPoint.getName(), terminationPoint.getPortUuid());
                }
            } else {
                LOG.warn("OVSDB bridge node was not found: {}", iid);
            }
        }
    }

    private static void updateInterface(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {
        updateOfPort(terminationPoint, ovsInterface);
        updateOfPortRequest(terminationPoint, ovsInterface);
        updateInterfaceOptions(terminationPoint, ovsInterface);
        updateInterfaceOtherConfig(terminationPoint, ovsInterface);
        updateInterfaceExternalIds(terminationPoint, ovsInterface);
        updateInterfaceLldp(terminationPoint, ovsInterface);
        updateInterfaceBfd(terminationPoint, ovsInterface);
        updateInterfacePolicing(terminationPoint, ovsInterface);
    }

    private static void updatePort(final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port, final OvsdbBridgeAugmentation operBridge, final String opendaylightIid) {
        updatePortOtherConfig(terminationPoint, port);
        updatePortVlanTag(terminationPoint, port);
        updatePortVlanTrunk(terminationPoint, port);
        updatePortVlanMode(terminationPoint, port);
        updatePortExternalIds(terminationPoint, port, opendaylightIid);
        updatePortQos(terminationPoint, port, operBridge);
    }

    private static void updatePortQos(final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port, final OvsdbBridgeAugmentation operBridge) {

        Set<UUID> uuidSet = new HashSet<>();

        // First check if QosEntry is present and use that
        if (terminationPoint.getQosEntry() != null && !terminationPoint.getQosEntry().isEmpty()) {
            OvsdbQosRef qosRef = terminationPoint.getQosEntry().values().iterator().next().getQosRef();
            Uri qosId = qosRef.getValue().firstKeyOf(QosEntries.class).getQosId();
            OvsdbNodeAugmentation operNode = getOperNode(operBridge);
            if (operNode != null) {
                Map<QosEntriesKey, QosEntries> entries = operNode.getQosEntries();
                if (entries != null) {
                    QosEntries qosEntry = entries.get(new QosEntriesKey(qosId));
                    if (qosEntry != null) {
                        uuidSet.add(new UUID(qosEntry.getQosUuid().getValue()));
                    }
                }
            }
            if (uuidSet.size() == 0) {
                uuidSet.add(new UUID(SouthboundConstants.QOS_NAMED_UUID_PREFIX
                        + TransactUtils.bytesToHexString(qosId.getValue().getBytes(UTF_8))));
            }
        }
        port.setQos(uuidSet);
    }

    @SuppressWarnings("IllegalCatch")
    private static OvsdbNodeAugmentation getOperNode(final OvsdbBridgeAugmentation operBridge) {
        @SuppressWarnings("unchecked")
        InstanceIdentifier<Node> iidNode = (InstanceIdentifier<Node>)operBridge.getManagedBy().getValue();
        OvsdbNodeAugmentation operNode = null;
        try (ReadTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction()) {
            Optional<Node> nodeOptional = SouthboundUtil.readNode(transaction, iidNode);
            if (nodeOptional.isPresent()) {
                operNode = nodeOptional.orElseThrow().augmentation(OvsdbNodeAugmentation.class);
            }
        } catch (Exception exp) {
            LOG.error("Error in getting the brideNode for {}", iidNode, exp);
        }
        return operNode;
    }

    private static void updateOfPort(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Uint32 ofPort = terminationPoint.getOfport();
        if (ofPort != null) {
            ovsInterface.setOpenFlowPort(Collections.singleton(ofPort.toJava()));
        }
    }

    private static void updateOfPortRequest(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Uint16 ofPortRequest = terminationPoint.getOfportRequest();
        if (ofPortRequest != null) {
            ovsInterface.setOpenFlowPortRequest(Collections.singleton(ofPortRequest.longValue()));
        }
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updateInterfaceOptions(final OvsdbTerminationPointAugmentation terminationPoint,
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

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updateInterfaceExternalIds(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Map<InterfaceExternalIdsKey, InterfaceExternalIds> interfaceExternalIds =
                terminationPoint.getInterfaceExternalIds();
        final InterfaceExternalIds odl = SouthboundUtil.interfaceCreatedByOpenDaylight();

        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            interfaceExternalIds.put(odl.key(), odl);
        } else {
            interfaceExternalIds = Map.of(odl.key(), odl);
        }
        try {
            ovsInterface.setExternalIds(YangUtils.convertYangKeyValueListToMap(interfaceExternalIds,
                    InterfaceExternalIds::getExternalIdKey, InterfaceExternalIds::getExternalIdValue));
        } catch (NullPointerException e) {
            LOG.warn("Incomplete OVSDB interface external_ids", e);
        }
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updateInterfaceLldp(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {
        try {
            Map<InterfaceLldpKey, InterfaceLldp> interfaceLldpList =
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

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updateInterfaceOtherConfig(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Map<InterfaceOtherConfigsKey, InterfaceOtherConfigs> interfaceOtherConfigs =
                terminationPoint.getInterfaceOtherConfigs();
        if (interfaceOtherConfigs != null && !interfaceOtherConfigs.isEmpty()) {
            Map<String, String> otherConfigsMap = new HashMap<>();
            for (InterfaceOtherConfigs interfaceOtherConfig : interfaceOtherConfigs.values()) {
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

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updateInterfaceBfd(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        try {
            Map<InterfaceBfdKey, InterfaceBfd> interfaceBfdList = terminationPoint.getInterfaceBfd();
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

    private static void updateInterfacePolicing(final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Uint32 ingressPolicingRate = terminationPoint.getIngressPolicingRate();
        if (ingressPolicingRate != null) {
            ovsInterface.setIngressPolicingRate(ingressPolicingRate.toJava());
        }
        Uint32 ingressPolicingBurst = terminationPoint.getIngressPolicingBurst();
        if (ingressPolicingBurst != null) {
            ovsInterface.setIngressPolicingBurst(ingressPolicingBurst.toJava());
        }
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updatePortExternalIds(final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port, final String opendaylightIid) {

        Map<String, String> externalIdMap = new HashMap<>();
        externalIdMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, opendaylightIid);
        externalIdMap.put(SouthboundConstants.CREATED_BY, SouthboundConstants.ODL);
        try {
            YangUtils.copyYangKeyValueListToMap(externalIdMap, terminationPoint.getPortExternalIds(),
                    PortExternalIds::getExternalIdKey, PortExternalIds::getExternalIdValue);
        } catch (NullPointerException e) {
            LOG.warn("Incomplete OVSDB port external_ids", e);
        }
        port.setExternalIds(externalIdMap);
    }

    private static void updatePortVlanTag(final OvsdbTerminationPointAugmentation terminationPoint, final Port port) {
        if (terminationPoint.getVlanTag() != null) {
            Set<Long> vlanTag = new HashSet<>();
            vlanTag.add(terminationPoint.getVlanTag().getValue().longValue());
            port.setTag(vlanTag);
        }
    }

    private static void updatePortVlanTrunk(final OvsdbTerminationPointAugmentation terminationPoint, final Port port) {
        if (terminationPoint.getTrunks() != null && terminationPoint.getTrunks().size() > 0) {
            Set<Long> portTrunks = new HashSet<>();
            List<Trunks> modelTrunks = terminationPoint.getTrunks();
            for (Trunks trunk : modelTrunks) {
                if (trunk.getTrunk() != null) {
                    portTrunks.add(trunk.getTrunk().getValue().longValue());
                }
            }
            port.setTrunks(portTrunks);
        }
    }

    private static void updatePortVlanMode(final OvsdbTerminationPointAugmentation terminationPoint, final Port port) {
        if (terminationPoint.getVlanMode() != null) {
            Set<String> portVlanMode = new HashSet<>();
            VlanMode modelVlanMode = terminationPoint.getVlanMode();
            portVlanMode.add(SouthboundConstants.VlanModes.values()[modelVlanMode.getIntValue() - 1].getMode());
            port.setVlanMode(portVlanMode);
        }
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void updatePortOtherConfig(final OvsdbTerminationPointAugmentation terminationPoint,
            final Port ovsPort) {
        Map<PortOtherConfigsKey, PortOtherConfigs> portOtherConfigs =
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
}
