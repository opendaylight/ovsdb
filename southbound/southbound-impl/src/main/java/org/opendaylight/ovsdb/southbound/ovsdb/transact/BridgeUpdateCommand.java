/*
 * Copyright © 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, OvsdbBridgeAugmentation.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbBridgeAugmentation.class),
                instanceIdentifierCodec);
    }

    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> createdOrUpdated,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> ovsdbManagedNodeEntry :
                createdOrUpdated.entrySet()) {
            updateBridge(transaction, state, ovsdbManagedNodeEntry.getKey(), ovsdbManagedNodeEntry.getValue(),
                    instanceIdentifierCodec);
        }
    }

    private static void updateBridge(final TransactionBuilder transaction, final BridgeOperationalState state,
            final InstanceIdentifier<OvsdbBridgeAugmentation> iid, final OvsdbBridgeAugmentation ovsdbManagedNode,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        LOG.debug("Received request to create ovsdb bridge name: {} uuid: {}",
                ovsdbManagedNode.getBridgeName(),
                ovsdbManagedNode.getBridgeUuid());
        Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
        setFailMode(bridge, ovsdbManagedNode);
        setDataPathType(bridge, ovsdbManagedNode);
        setStpEnalbe(bridge, ovsdbManagedNode);
        setOpenDaylightExternalIds(bridge, iid, ovsdbManagedNode, instanceIdentifierCodec);
        setOpenDaylightOtherConfig(bridge, ovsdbManagedNode);
        Optional<OvsdbBridgeAugmentation> operationalBridgeOptional =
                state.getOvsdbBridgeAugmentation(iid);
        if (!operationalBridgeOptional.isPresent()) {
            setName(bridge, ovsdbManagedNode,operationalBridgeOptional);
            setPort(transaction, bridge, ovsdbManagedNode);
            transaction.add(op.insert(bridge));
            LOG.info("Added ovsdb Bridge name: {} uuid: {}",
                    ovsdbManagedNode.getBridgeName(),
                    ovsdbManagedNode.getBridgeUuid());
        } else {
            String existingBridgeName = operationalBridgeOptional.orElseThrow().getBridgeName().getValue();
            LOG.debug("Bridge {} already exists in device updating {}", existingBridgeName, iid);
            // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
            Bridge extraBridge = transaction.getTypedRowWrapper(Bridge.class);
            extraBridge.setName("");
            transaction.add(op.update(bridge)
                    .where(extraBridge.getNameColumn().getSchema().opEqual(existingBridgeName))
                    .build());
            stampInstanceIdentifier(transaction, iid.firstIdentifierOf(Node.class), existingBridgeName,
                    instanceIdentifierCodec);
        }
    }

    private static void setDataPathType(final Bridge bridge,final OvsdbBridgeAugmentation ovsdbManagedNode) {
        if (ovsdbManagedNode.getDatapathType() != null) {
            bridge.setDatapathType(SouthboundMapper.createDatapathType(ovsdbManagedNode));
        }
    }

    private static void setStpEnalbe(final Bridge bridge, final OvsdbBridgeAugmentation ovsdbManageNode) {
        if (ovsdbManageNode.getStpEnable() != null) {
            bridge.setStpEnable(ovsdbManageNode.getStpEnable());
        }
    }

    private static void setName(final Bridge bridge, final OvsdbBridgeAugmentation ovsdbManagedNode,
            final Optional<OvsdbBridgeAugmentation> operationalBridgeOptional) {
        if (ovsdbManagedNode.getBridgeName() != null) {
            bridge.setName(ovsdbManagedNode.getBridgeName().getValue());
        } else if (operationalBridgeOptional.isPresent()
            && operationalBridgeOptional.orElseThrow().getBridgeName() != null) {
            bridge.setName(operationalBridgeOptional.orElseThrow().getBridgeName().getValue());
        }
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void setOpenDaylightExternalIds(final Bridge bridge,
            final InstanceIdentifier<OvsdbBridgeAugmentation> iid, final OvsdbBridgeAugmentation ovsdbManagedNode,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        // Set the iid external_id
        Map<String, String> externalIdMap = new HashMap<>();
        externalIdMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, instanceIdentifierCodec.serialize(iid));
        // Set user provided external ids
        try {
            YangUtils.copyYangKeyValueListToMap(externalIdMap, ovsdbManagedNode.getBridgeExternalIds(),
                    BridgeExternalIds::getBridgeExternalIdKey, BridgeExternalIds::getBridgeExternalIdValue);
        } catch (NullPointerException e) {
            LOG.warn("Incomplete bridge external Id", e);
        }
        bridge.setExternalIds(externalIdMap);
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void setOpenDaylightOtherConfig(final @NonNull Bridge bridge,
            final @NonNull OvsdbBridgeAugmentation ovsdbManagedNode) {
        try {
            bridge.setOtherConfig(YangUtils.convertYangKeyValueListToMap(ovsdbManagedNode.getBridgeOtherConfigs(),
                    BridgeOtherConfigs::getBridgeOtherConfigKey, BridgeOtherConfigs::getBridgeOtherConfigValue));
        } catch (NullPointerException e) {
            LOG.warn("Incomplete bridge other config", e);
        }
    }

    private static void setPort(final TransactionBuilder transaction, final Bridge bridge,
            final OvsdbBridgeAugmentation ovsdbManagedNode) {

        Insert<GenericTableSchema> interfaceInsert = setInterface(transaction,ovsdbManagedNode);
        // Port part
        String portNamedUuid = "Port_" + SouthboundMapper.getRandomUuid();
        Port port = transaction.getTypedRowWrapper(Port.class);
        port.setName(ovsdbManagedNode.getBridgeName().getValue());
        port.setInterfaces(Set.of(TransactUtils.extractNamedUuid(interfaceInsert)));
        transaction.add(op.insert(port).withId(portNamedUuid));
        bridge.setPorts(Set.of(new UUID(portNamedUuid)));
    }

    private static Insert<GenericTableSchema> setInterface(final TransactionBuilder transaction,
            final OvsdbBridgeAugmentation ovsdbManagedNode) {
        // Interface part
        String interfaceNamedUuid = "Interface_" + SouthboundMapper.getRandomUuid();
        Interface interfaceOvs = transaction.getTypedRowWrapper(Interface.class);
        interfaceOvs.setName(ovsdbManagedNode.getBridgeName().getValue());
        interfaceOvs.setType(SouthboundMapper.createOvsdbInterfaceType(InterfaceTypeInternal.VALUE));
        Insert<GenericTableSchema> result = op.insert(interfaceOvs).withId(interfaceNamedUuid);
        transaction.add(result);
        return result;
    }

    private static void setFailMode(final Bridge bridge,
            final OvsdbBridgeAugmentation ovsdbManagedNode) {
        if (ovsdbManagedNode.getFailMode() != null
                && SouthboundConstants.OVSDB_FAIL_MODE_MAP.get(ovsdbManagedNode.getFailMode()) != null) {
            bridge.setFailMode(Set.of(
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.get(ovsdbManagedNode.getFailMode())));
        }
    }

    private static void stampInstanceIdentifier(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> iid, final String bridgeName,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
        bridge.setName(bridgeName);
        bridge.setExternalIds(Collections.emptyMap());
        Mutate mutate = TransactUtils.stampInstanceIdentifierMutation(transaction, iid, bridge.getSchema(),
                bridge.getExternalIdsColumn().getSchema(), instanceIdentifierCodec);
        transaction.add(mutate
                .where(bridge.getNameColumn().getSchema().opEqual(bridgeName))
                .build());
    }

}
