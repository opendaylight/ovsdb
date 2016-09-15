/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeUpdateCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, OvsdbBridgeAugmentation.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbBridgeAugmentation.class),
                instanceIdentifierCodec);
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> createdOrUpdated,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> ovsdbManagedNodeEntry :
                createdOrUpdated.entrySet()) {
            updateBridge(transaction, state, ovsdbManagedNodeEntry.getKey(), ovsdbManagedNodeEntry.getValue(),
                    instanceIdentifierCodec);
        }
    }

    private void updateBridge(TransactionBuilder transaction, BridgeOperationalState state,
            InstanceIdentifier<OvsdbBridgeAugmentation> iid, OvsdbBridgeAugmentation ovsdbManagedNode,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        LOG.debug("Received request to create ovsdb bridge name: {} uuid: {}",
                ovsdbManagedNode.getBridgeName(),
                ovsdbManagedNode.getBridgeUuid());
        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
        setFailMode(bridge, ovsdbManagedNode);
        setDataPathType(bridge, ovsdbManagedNode);
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
            String existingBridgeName = operationalBridgeOptional.get().getBridgeName().getValue();
            // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
            Bridge extraBridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
            extraBridge.setName("");
            transaction.add(op.update(bridge)
                    .where(extraBridge.getNameColumn().getSchema().opEqual(existingBridgeName))
                    .build());
            stampInstanceIdentifier(transaction, iid.firstIdentifierOf(Node.class), existingBridgeName,
                    instanceIdentifierCodec);
        }
    }

    private void setDataPathType(Bridge bridge,OvsdbBridgeAugmentation ovsdbManagedNode) {
        if (ovsdbManagedNode.getDatapathType() != null) {
            bridge.setDatapathType(SouthboundMapper.createDatapathType(ovsdbManagedNode));
        }
    }

    private void setName(Bridge bridge, OvsdbBridgeAugmentation ovsdbManagedNode,
            Optional<OvsdbBridgeAugmentation> operationalBridgeOptional) {
        if (ovsdbManagedNode.getBridgeName() != null) {
            bridge.setName(ovsdbManagedNode.getBridgeName().getValue());
        } else if (operationalBridgeOptional.isPresent() && operationalBridgeOptional.get().getBridgeName() != null) {
            bridge.setName(operationalBridgeOptional.get().getBridgeName().getValue());
        }
    }

    private void setOpenDaylightExternalIds(Bridge bridge, InstanceIdentifier<OvsdbBridgeAugmentation> iid,
            OvsdbBridgeAugmentation ovsdbManagedNode, InstanceIdentifierCodec instanceIdentifierCodec) {
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

    private void setOpenDaylightOtherConfig(@Nonnull Bridge bridge, @Nonnull OvsdbBridgeAugmentation ovsdbManagedNode) {
        try {
            bridge.setOtherConfig(YangUtils.convertYangKeyValueListToMap(ovsdbManagedNode.getBridgeOtherConfigs(),
                    BridgeOtherConfigs::getBridgeOtherConfigKey, BridgeOtherConfigs::getBridgeOtherConfigValue));
        } catch (NullPointerException e) {
            LOG.warn("Incomplete bridge other config", e);
        }
    }

    private void setPort(TransactionBuilder transaction, Bridge bridge,
            OvsdbBridgeAugmentation ovsdbManagedNode) {

        Insert<GenericTableSchema> interfaceInsert = setInterface(transaction,ovsdbManagedNode);
        // Port part
        String portNamedUuid = "Port_" + SouthboundMapper.getRandomUuid();
        Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
        port.setName(ovsdbManagedNode.getBridgeName().getValue());
        port.setInterfaces(Sets.newHashSet(TransactUtils.extractNamedUuid(interfaceInsert)));
        transaction.add(op.insert(port).withId(portNamedUuid));
        bridge.setPorts(Sets.newHashSet(new UUID(portNamedUuid)));
    }

    private Insert<GenericTableSchema> setInterface(TransactionBuilder transaction,
            OvsdbBridgeAugmentation ovsdbManagedNode) {
        // Interface part
        String interfaceNamedUuid = "Interface_" + SouthboundMapper.getRandomUuid();
        Interface interfaceOvs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Interface.class);
        interfaceOvs.setName(ovsdbManagedNode.getBridgeName().getValue());
        interfaceOvs.setType(SouthboundMapper.createOvsdbInterfaceType(InterfaceTypeInternal.class));
        Insert<GenericTableSchema> result = op.insert(interfaceOvs).withId(interfaceNamedUuid);
        transaction.add(result);
        return result;
    }

    private void setFailMode(Bridge bridge,
            OvsdbBridgeAugmentation ovsdbManagedNode) {
        if (ovsdbManagedNode.getFailMode() != null
                && SouthboundConstants.OVSDB_FAIL_MODE_MAP.get(ovsdbManagedNode.getFailMode()) != null ) {
            bridge.setFailMode(Sets.newHashSet(
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.get(ovsdbManagedNode.getFailMode())));
        }
    }

    private void stampInstanceIdentifier(TransactionBuilder transaction,InstanceIdentifier<Node> iid,
            String bridgeName, InstanceIdentifierCodec instanceIdentifierCodec) {
        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
        bridge.setName(bridgeName);
        bridge.setExternalIds(Collections.<String,String>emptyMap());
        Mutate mutate = TransactUtils.stampInstanceIdentifierMutation(transaction, iid, bridge.getSchema(),
                bridge.getExternalIdsColumn().getSchema(), instanceIdentifierCodec);
        transaction.add(mutate
                .where(bridge.getNameColumn().getSchema().opEqual(bridgeName))
                .build());
    }

}