/*
 * Copyright © 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQosRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes._interface.list.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes._interface.list.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes._interface.list.InterfaceLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes._interface.list.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes._interface.list.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationPointUpdateCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreatedOrUpdated(events, OvsdbTerminationPointAugmentation.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbTerminationPointAugmentation.class),
                instanceIdentifierCodec);
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                    createdOrUpdated,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<OvsdbTerminationPointAugmentation>,
                 OvsdbTerminationPointAugmentation> terminationPointEntry : createdOrUpdated.entrySet()) {
            updateTerminationPoint(transaction, state, terminationPointEntry.getKey(),
                    terminationPointEntry.getValue(), instanceIdentifierCodec);
        }
    }

    public void updateTerminationPoint(TransactionBuilder transaction, BridgeOperationalState state,
            InstanceIdentifier<OvsdbTerminationPointAugmentation> iid,
            OvsdbTerminationPointAugmentation terminationPoint, InstanceIdentifierCodec instanceIdentifierCodec) {

        if (terminationPoint != null) {
            LOG.debug("Received request to update termination point {}",
                   terminationPoint.getName());

            List<InterfaceList> interfaceList = terminationPoint.getInterfaceList();
            if (interfaceList != null && !interfaceList.isEmpty()) {
                for (InterfaceList interf : interfaceList) {
                    // Update interface
                    Interface ovsInterface =
                            TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Interface.class);
                    LOG.debug("Received request to update interface {}", interf.getName());
                    updateInterface(interf, ovsInterface);
                    Interface extraInterface = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), Interface.class);
                    extraInterface.setName("");
                    transaction.add(op.update(ovsInterface)
                            .where(extraInterface.getNameColumn().getSchema().opEqual(interf.getName()))
                            .build());
                }
            }

            TerminationPointCreateCommand.stampInstanceIdentifier(transaction,
                    iid.firstIdentifierOf(OvsdbTerminationPointAugmentation.class), terminationPoint.getName(),
                    instanceIdentifierCodec);

            // Update port
            // Bug#6136
            Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional = state.getOvsdbBridgeAugmentation(iid);
            if (ovsdbBridgeOptional != null && ovsdbBridgeOptional.isPresent()) {
                OvsdbBridgeAugmentation operBridge = ovsdbBridgeOptional.get();
                if (operBridge != null) {
                    Port port = TyperUtils.getTypedRowWrapper(
                        transaction.getDatabaseSchema(), Port.class);
                    updatePort(terminationPoint, port, operBridge);
                    Port extraPort = TyperUtils.getTypedRowWrapper(
                        transaction.getDatabaseSchema(), Port.class);
                    extraPort.setName("");
                    transaction.add(op.update(port)
                        .where(extraPort.getNameColumn().getSchema().opEqual(terminationPoint.getName()))
                        .build());
                    LOG.info("Updated Termination Point : {}  with Uuid : {}",
                        terminationPoint.getName(), terminationPoint.getPortUuid());
                }
            } else {
                LOG.warn("OVSDB bridge node was not found: {}", iid);
            }
        }
    }

    private void updateInterface(
            final InterfaceList interf,
            final Interface ovsInterface) {
        //ovsInterface.setName(interf.getName()); //immutable column for ovsdb
        updateOfPort(interf, ovsInterface);
        updateOfPortRequest(interf, ovsInterface);
        updateInterfaceOptions(interf, ovsInterface);
        updateInterfaceOtherConfig(interf, ovsInterface);
        updateInterfaceExternalIds(interf, ovsInterface);
        updateInterfaceLldp(interf, ovsInterface);
        updateInterfaceBfd(interf, ovsInterface);
        updateInterfacePolicing(interf, ovsInterface);
    }

    private void updatePort(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port,
            final OvsdbBridgeAugmentation operBridge) {

        updatePortOtherConfig(terminationPoint, port);
        updatePortVlanTag(terminationPoint, port);
        updatePortVlanTrunk(terminationPoint, port);
        updatePortVlanMode(terminationPoint, port);
        updatePortExternalIds(terminationPoint, port);
        updatePortQos(terminationPoint, port, operBridge);
    }

    private void updatePortQos(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port,
            final OvsdbBridgeAugmentation operBridge) {

        Set<UUID> uuidSet = new HashSet<>();

        // First check if QosEntry is present and use that
        if (terminationPoint.getQosEntry() != null && !terminationPoint.getQosEntry().isEmpty()) {
            OvsdbQosRef qosRef = terminationPoint.getQosEntry().iterator().next().getQosRef();
            Uri qosId = qosRef.getValue().firstKeyOf(QosEntries.class).getQosId();
            OvsdbNodeAugmentation operNode = getOperNode(operBridge);
            if (operNode != null && operNode.getQosEntries() != null
                    && !operNode.getQosEntries().isEmpty()) {
                for (QosEntries qosEntry : operNode.getQosEntries()) {
                    if (qosEntry.getQosId().equals(qosId)) {
                        uuidSet.add(new UUID(qosEntry.getQosUuid().getValue()));
                    }
                }
            }
            if (uuidSet.size() == 0) {
                uuidSet.add(new UUID(SouthboundConstants.QOS_NAMED_UUID_PREFIX
                        + TransactUtils.bytesToHexString(qosId.getValue().getBytes())));
            }
        }
        port.setQos(uuidSet);
    }

    private OvsdbNodeAugmentation getOperNode(final OvsdbBridgeAugmentation operBridge) {
        @SuppressWarnings("unchecked")
        InstanceIdentifier<Node> iidNode = (InstanceIdentifier<Node>)operBridge.getManagedBy().getValue();
        OvsdbNodeAugmentation operNode = null;
        ReadOnlyTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, iidNode);
        try {
            Optional<Node> nodeOptional = future.get();
            if (nodeOptional.isPresent()) {
                operNode = nodeOptional.get().getAugmentation(OvsdbNodeAugmentation.class);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Error reading from datastore", e);
        }
        return operNode;
    }

    private void updateOfPort(
            final InterfaceList interf,
            final Interface ovsInterface) {

        Long ofPort = interf.getOfport();
        if (ofPort != null) {
            ovsInterface.setOpenFlowPort(Collections.singleton(ofPort));
        }
    }

    private void updateOfPortRequest(
            final InterfaceList interf,
            final Interface ovsInterface) {

        Integer ofPortRequest = interf.getOfportRequest();
        if (ofPortRequest != null) {
            ovsInterface.setOpenFlowPortRequest(Collections.singleton(ofPortRequest.longValue()));
        }
    }

    private void updateInterfaceOptions(
            final InterfaceList interf,
            final Interface ovsInterface) {

        //Configure optional input
        if (interf.getOptions() != null) {
            try {
                ovsInterface.setOptions(YangUtils.convertYangKeyValueListToMap(interf.getOptions(),
                        Options::getOption, Options::getValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface options", e);
            }
        }
    }

    private void updateInterfaceExternalIds(
            final InterfaceList interf,
            final Interface ovsInterface) {

        List<InterfaceExternalIds> interfaceExternalIds =
                interf.getInterfaceExternalIds();
        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            try {
                ovsInterface.setExternalIds(YangUtils.convertYangKeyValueListToMap(interfaceExternalIds,
                        InterfaceExternalIds::getExternalIdKey, InterfaceExternalIds::getExternalIdValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface external_ids", e);
            }
        }
    }

    private void updateInterfaceLldp(
            final InterfaceList interf,
            final Interface ovsInterface) {

        try {
            List<InterfaceLldp> interfaceLldpList =
                    interf.getInterfaceLldp();
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

    private void updateInterfaceOtherConfig(
            final InterfaceList interf,
            final Interface ovsInterface) {

        List<InterfaceOtherConfigs> interfaceOtherConfigs =
                interf.getInterfaceOtherConfigs();
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

    private void updateInterfaceBfd(
            final InterfaceList interf,
            final Interface ovsInterface) {

        try {
            List<InterfaceBfd> interfaceBfdList =
                    interf.getInterfaceBfd();
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

    private void updateInterfacePolicing(
            final InterfaceList interf,
            final Interface ovsInterface) {

        Long ingressPolicingRate = interf.getIngressPolicingRate();
        if (ingressPolicingRate != null) {
            ovsInterface.setIngressPolicingRate(ingressPolicingRate);
        }
        Long ingressPolicingBurst = interf.getIngressPolicingBurst();
        if (ingressPolicingBurst != null) {
            ovsInterface.setIngressPolicingBurst(ingressPolicingBurst);
        }
    }

    private void updatePortExternalIds(
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

    private void updatePortVlanTag(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        if (terminationPoint.getVlanTag() != null) {
            Set<Long> vlanTag = new HashSet<>();
            vlanTag.add(terminationPoint.getVlanTag().getValue().longValue());
            port.setTag(vlanTag);
        }
    }

    private void updatePortVlanTrunk(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

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

    private void updatePortVlanMode(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {
        if (terminationPoint.getVlanMode() != null) {
            Set<String> portVlanMode = new HashSet<>();
            VlanMode modelVlanMode = terminationPoint.getVlanMode();
            portVlanMode.add(SouthboundConstants.VlanModes.values()[modelVlanMode.getIntValue() - 1].getMode());
            port.setVlanMode(portVlanMode);
        }
    }

    private void updatePortOtherConfig(
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

}
