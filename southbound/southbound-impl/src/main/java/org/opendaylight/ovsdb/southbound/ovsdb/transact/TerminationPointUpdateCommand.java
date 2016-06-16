/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class TerminationPointUpdateCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        execute(transaction, TransactUtils.extractCreatedOrUpdated(events, OvsdbTerminationPointAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        Collection<DataTreeModification<Node>> modifications) {
        execute(transaction,
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbTerminationPointAugmentation.class));
    }

    private void execute(TransactionBuilder transaction,
                         Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>,
                                 OvsdbTerminationPointAugmentation> createdOrUpdated) {
        LOG.trace("TerminationPointUpdateCommand called");
        for (Entry<InstanceIdentifier<OvsdbTerminationPointAugmentation>,
                OvsdbTerminationPointAugmentation> terminationPointEntry : createdOrUpdated.entrySet()) {
            updateTerminationPoint(transaction, terminationPointEntry.getKey(), terminationPointEntry.getValue());
        }
    }

    public void updateTerminationPoint(TransactionBuilder transaction,
                                       InstanceIdentifier<OvsdbTerminationPointAugmentation> iid,
                                       OvsdbTerminationPointAugmentation terminationPoint) {
        if (terminationPoint != null) {
            LOG.debug("Received request to update termination point {}",
                    terminationPoint.getName());

            // Update interface
            Interface ovsInterface =
                    TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Interface.class);
            updateInterface(terminationPoint, ovsInterface);
            Interface extraInterface = TyperUtils.getTypedRowWrapper(
                    transaction.getDatabaseSchema(), Interface.class);
            extraInterface.setName("");
            transaction.add(op.update(ovsInterface)
                    .where(extraInterface.getNameColumn().getSchema().opEqual(terminationPoint.getName()))
                    .build());

            TerminationPointCreateCommand.stampInstanceIdentifier(transaction,
                    iid.firstIdentifierOf(OvsdbTerminationPointAugmentation.class), terminationPoint.getName());

            // Update port
            Port port = TyperUtils.getTypedRowWrapper(
                    transaction.getDatabaseSchema(), Port.class);
            updatePort(terminationPoint, port);
            Port extraPort = TyperUtils.getTypedRowWrapper(
                    transaction.getDatabaseSchema(), Port.class);
            extraPort.setName("");
            transaction.add(op.update(port)
                    .where(extraPort.getNameColumn().getSchema().opEqual(terminationPoint.getName()))
                    .build());
        }
    }

    private void updateInterface(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {
        updateOfPort(terminationPoint, ovsInterface);
        updateOfPortRequest(terminationPoint, ovsInterface);
        updateInterfaceOptions(terminationPoint, ovsInterface);
        updateInterfaceOtherConfig(terminationPoint, ovsInterface);
        updateInterfaceExternalIds(terminationPoint, ovsInterface);
        updateInterfaceLldp(terminationPoint, ovsInterface);
        updateInterfaceBfd(terminationPoint, ovsInterface);
    }

    private void updatePort(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        updatePortOtherConfig(terminationPoint, port);
        updatePortVlanTag(terminationPoint, port);
        updatePortVlanTrunk(terminationPoint, port);
        updatePortVlanMode(terminationPoint, port);
        updatePortExternalIds(terminationPoint, port);
        updatePortQos(terminationPoint, port);
    }

    private void updatePortQos(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        Set<UUID> uuidSet = Sets.newHashSet();
        Uuid qosUuid = terminationPoint.getQos();
        if (qosUuid != null) {
            uuidSet.add(new UUID(qosUuid.getValue()));
        }
        port.setQos(uuidSet);
    }


    private void updateOfPort(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Long ofPort = terminationPoint.getOfport();
        if (ofPort != null) {
            ovsInterface.setOpenFlowPort(Sets.newHashSet(ofPort));
        }
    }

    private void updateOfPortRequest(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Integer ofPortRequest = terminationPoint.getOfportRequest();
        if (ofPortRequest != null) {
            ovsInterface.setOpenFlowPortRequest(Sets.newHashSet(ofPortRequest.longValue()));
        }
    }

    private void updateInterfaceOptions(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        //Configure optional input
        if (terminationPoint.getOptions() != null) {
            Map<String, String> optionsMap = new HashMap<>();
            for (Options option : terminationPoint.getOptions()) {
                optionsMap.put(option.getOption(), option.getValue());
            }
            try {
                ovsInterface.setOptions(ImmutableMap.copyOf(optionsMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface options", e);
            }
        }
    }

    private void updateInterfaceExternalIds(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        List<InterfaceExternalIds> interfaceExternalIds =
                terminationPoint.getInterfaceExternalIds();
        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            Map<String, String> externalIdsMap = new HashMap<>();
            for (InterfaceExternalIds externalId: interfaceExternalIds) {
                externalIdsMap.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
            }
            try {
                ovsInterface.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface external_ids", e);
            }
        }
    }

    private void updateInterfaceLldp(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        try {
            List<InterfaceLldp> interfaceLldpList =
                    terminationPoint.getInterfaceLldp();
            if (interfaceLldpList != null && !interfaceLldpList.isEmpty()) {
                Map<String, String> interfaceLldpMap = new HashMap<>();
                for (InterfaceLldp interfaceLldp : interfaceLldpList) {
                    interfaceLldpMap.put(interfaceLldp.getLldpKey(), interfaceLldp.getLldpValue());
                }
                try {
                    ovsInterface.setLldp(ImmutableMap.copyOf(interfaceLldpMap));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB interface lldp", e);
                }
            }
        } catch (SchemaVersionMismatchException e) {
            SouthboundUtil.schemaMismatchlog("lldp", "Interface", e);
        }
    }

    private void updateInterfaceOtherConfig(
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

    private void updateInterfaceBfd(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        try {
            List<InterfaceBfd> interfaceBfdList =
                    terminationPoint.getInterfaceBfd();
            if (interfaceBfdList != null && !interfaceBfdList.isEmpty()) {
                Map<String, String> interfaceBfdMap = new HashMap<>();
                for (InterfaceBfd interfaceBfd : interfaceBfdList) {
                    interfaceBfdMap.put(interfaceBfd.getBfdKey(), interfaceBfd.getBfdValue());
                }
                try {
                    ovsInterface.setBfd(ImmutableMap.copyOf(interfaceBfdMap));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB interface bfd", e);
                }
            }
        } catch (SchemaVersionMismatchException e) {
            SouthboundUtil.schemaMismatchlog("bfd", "Interface", e);
        }
    }

    private void updatePortExternalIds(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        List<PortExternalIds> portExternalIds = terminationPoint.getPortExternalIds();
        if (portExternalIds != null && !portExternalIds.isEmpty()) {
            Map<String, String> externalIdsMap = new HashMap<>();
            for (PortExternalIds externalId: portExternalIds) {
                externalIdsMap.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
            }
            try {
                port.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
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
            portVlanMode.add(SouthboundConstants.VLANMODES.values()[modelVlanMode.getIntValue() - 1].getMode());
            port.setVlanMode(portVlanMode);
        }
    }

    private void updatePortOtherConfig(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port ovsPort) {
        List<PortOtherConfigs> portOtherConfigs =
                terminationPoint.getPortOtherConfigs();
        if (portOtherConfigs != null && !portOtherConfigs.isEmpty()) {
            Map<String, String> otherConfigsMap = new HashMap<>();
            for (PortOtherConfigs portOtherConfig : portOtherConfigs) {
                otherConfigsMap.put(portOtherConfig.getOtherConfigKey(),
                        portOtherConfig.getOtherConfigValue());
            }
            try {
                ovsPort.setOtherConfig(ImmutableMap.copyOf(otherConfigsMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB port other_config", e);
            }
        }
    }

}
