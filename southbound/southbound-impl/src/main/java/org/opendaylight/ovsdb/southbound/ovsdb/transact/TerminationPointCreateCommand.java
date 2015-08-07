/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;

public class TerminationPointCreateCommand extends AbstractTransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointCreateCommand.class);

    public TerminationPointCreateCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (Entry<InstanceIdentifier<?>, DataObject> entry: getChanges().getCreatedData().entrySet()) {
            DataObject dataObject = entry.getValue();
            if (dataObject instanceof OvsdbTerminationPointAugmentation) {
                OvsdbTerminationPointAugmentation terminationPoint = (OvsdbTerminationPointAugmentation) dataObject;
                LOG.debug("Received request to create termination point {}",
                        terminationPoint.getName());
                InstanceIdentifier terminationPointIid = entry.getKey();
                Optional<TerminationPoint> terminationPointOptional =
                        getOperationalState().getBridgeTerminationPoint(terminationPointIid);
                if (!terminationPointOptional.isPresent()) {
                    // Configure interface
                    String interfaceUuid = "Interface_" + SouthboundMapper.getRandomUUID();;
                    Interface ovsInterface =
                            TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Interface.class);
                    createInterface(terminationPoint, ovsInterface);
                    transaction.add(op.insert(ovsInterface).withId(interfaceUuid));

                    stampInstanceIdentifier(transaction, (InstanceIdentifier<TerminationPoint>) entry.getKey(),
                            ovsInterface.getName());

                    // Configure port with the above interface details
                    String portUuid = "Port_" + SouthboundMapper.getRandomUUID();
                    Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
                    createPort(terminationPoint, port, interfaceUuid);
                    transaction.add(op.insert(port).withId(portUuid));

                    //Configure bridge with the above port details
                    Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                    if (getBridge(entry.getKey()) != null) {
                        bridge.setName(getBridge(entry.getKey()).getBridgeName().getValue());
                        bridge.setPorts(Sets.newHashSet(new UUID(portUuid)));

                        transaction.add(op.mutate(bridge)
                                .addMutation(bridge.getPortsColumn().getSchema(),
                                        Mutator.INSERT,bridge.getPortsColumn().getData())
                                .where(bridge.getNameColumn().getSchema()
                                        .opEqual(bridge.getNameColumn().getData())).build());
                    }
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
        port.setInterfaces(Sets.newHashSet(new UUID(interfaceUuid)));
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
            ovsInterface.setOpenFlowPort(Sets.newHashSet(ofPort));
        }
    }

    private void createOfPortRequest(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        Integer ofPortRequest = terminationPoint.getOfportRequest();
        if (ofPortRequest != null) {
            ovsInterface.setOpenFlowPortRequest(Sets.newHashSet(ofPortRequest.longValue()));
        }
    }

    private void createInterfaceOptions(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        //Configure optional input
        if (terminationPoint.getOptions() != null) {
            Map<String, String> optionsMap = new HashMap<String, String>();
            for (Options option : terminationPoint.getOptions()) {
                optionsMap.put(option.getOption(), option.getValue());
            }
            try {
                ovsInterface.setOptions(ImmutableMap.copyOf(optionsMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface options");
            }
        }
    }

    private void createInterfaceExternalIds(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        List<InterfaceExternalIds> interfaceExternalIds =
                terminationPoint.getInterfaceExternalIds();
        if (interfaceExternalIds != null && !interfaceExternalIds.isEmpty()) {
            Map<String, String> externalIdsMap = new HashMap<String, String>();
            for (InterfaceExternalIds externalId: interfaceExternalIds) {
                externalIdsMap.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
            }
            try {
                ovsInterface.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB interface external_ids");
            }
        }
    }

    private void createInterfaceOtherConfig(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Interface ovsInterface) {

        List<InterfaceOtherConfigs> interfaceOtherConfigs =
                terminationPoint.getInterfaceOtherConfigs();
        if (interfaceOtherConfigs != null && !interfaceOtherConfigs.isEmpty()) {
            Map<String, String> otherConfigsMap = new HashMap<String, String>();
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

    private void createPortExternalIds(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        List<PortExternalIds> portExternalIds = terminationPoint.getPortExternalIds();
        if (portExternalIds != null && !portExternalIds.isEmpty()) {
            Map<String, String> externalIdsMap = new HashMap<String, String>();
            for (PortExternalIds externalId: portExternalIds) {
                externalIdsMap.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
            }
            try {
                port.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB port external_ids");
            }
        }
    }

    private void createPortVlanTag(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        if (terminationPoint.getVlanTag() != null) {
            Set<Long> vlanTag = new HashSet<Long>();
            vlanTag.add(terminationPoint.getVlanTag().getValue().longValue());
            port.setTag(vlanTag);
        }
    }

    private void createPortVlanTrunk(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port port) {

        if (terminationPoint.getTrunks() != null && terminationPoint.getTrunks().size() > 0) {
            Set<Long> portTrunks = new HashSet<Long>();
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
            Set<String> portVlanMode = new HashSet<String>();
            VlanMode modelVlanMode = terminationPoint.getVlanMode();
            portVlanMode.add(SouthboundConstants.VLANMODES.values()[modelVlanMode.getIntValue() - 1].getMode());
            port.setVlanMode(portVlanMode);
        }
    }

    private void createPortOtherConfig(
            final OvsdbTerminationPointAugmentation terminationPoint,
            final Port ovsPort) {
        List<PortOtherConfigs> portOtherConfigs =
                terminationPoint.getPortOtherConfigs();
        if (portOtherConfigs != null && !portOtherConfigs.isEmpty()) {
            Map<String, String> otherConfigsMap = new HashMap<String, String>();
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

    private OvsdbBridgeAugmentation getBridge(InstanceIdentifier<?> key) {
        OvsdbBridgeAugmentation bridge = null;
        InstanceIdentifier<Node> nodeIid = key.firstIdentifierOf(Node.class);
        Map<InstanceIdentifier<Node>, Node> nodes =
                TransactUtils.extractCreatedOrUpdated(getChanges(),Node.class);
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

    public static void stampInstanceIdentifier(TransactionBuilder transaction,InstanceIdentifier<TerminationPoint> iid,
            String interfaceName) {
        Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
        port.setName(interfaceName);;
        port.setExternalIds(Collections.<String,String>emptyMap());
        Mutate mutate = TransactUtils.stampInstanceIdentifierMutation(transaction,
                iid,
                port.getSchema(),
                port.getExternalIdsColumn().getSchema());
        transaction.add(mutate
                .where(port.getNameColumn().getSchema().opEqual(interfaceName))
                .build());
    }

}
