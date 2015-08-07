/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class BridgeUpdateCommand extends AbstractTransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeUpdateCommand.class);

    public BridgeUpdateCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }



    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> created =
                TransactUtils.extractCreated(getChanges(),OvsdbBridgeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> ovsdbManagedNodeEntry:
            created.entrySet()) {
            updateBridge(transaction,  ovsdbManagedNodeEntry.getKey(), ovsdbManagedNodeEntry.getValue());
        }
        Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updated =
                TransactUtils.extractUpdated(getChanges(),OvsdbBridgeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> ovsdbManagedNodeEntry:
            updated.entrySet()) {
            updateBridge(transaction,  ovsdbManagedNodeEntry.getKey(), ovsdbManagedNodeEntry.getValue());
        }
    }



    private void updateBridge(
            TransactionBuilder transaction,
            InstanceIdentifier<OvsdbBridgeAugmentation> iid, OvsdbBridgeAugmentation ovsdbManagedNode) {
        LOG.debug("Received request to create ovsdb bridge name: {} uuid: {}",
                    ovsdbManagedNode.getBridgeName(),
                    ovsdbManagedNode.getBridgeUuid());
        Optional<OvsdbBridgeAugmentation> operationalBridgeOptional =
                getOperationalState().getOvsdbBridgeAugmentation(iid);
        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
        setFailMode(bridge, ovsdbManagedNode);
        setDataPathType(bridge, ovsdbManagedNode);
        setOpenDaylightExternalIds(bridge, iid, ovsdbManagedNode);
        setOpenDaylightOtherConfig(bridge, ovsdbManagedNode);
        if (!operationalBridgeOptional.isPresent()) {
            setName(bridge, ovsdbManagedNode,operationalBridgeOptional);
            setPort(transaction, bridge, ovsdbManagedNode);
            transaction.add(op.insert(bridge));
        } else {
            String existingBridgeName = operationalBridgeOptional.get().getBridgeName().getValue();
            // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
            Bridge extraBridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
            extraBridge.setName("");
            transaction.add(op.update(bridge)
                    .where(extraBridge.getNameColumn().getSchema().opEqual(existingBridgeName))
                    .build());
            stampInstanceIdentifier(transaction, iid.firstIdentifierOf(Node.class),existingBridgeName);
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
            OvsdbBridgeAugmentation ovsdbManagedNode) {
        // Set the iid external_id
        Map<String, String> externalIdMap = new HashMap<String, String>();
        externalIdMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, SouthboundUtil.serializeInstanceIdentifier(iid));
        // Set user provided external ids
        List<BridgeExternalIds> bridgeExternalId = ovsdbManagedNode.getBridgeExternalIds();
        if (bridgeExternalId != null) {
            for (BridgeExternalIds externalId : bridgeExternalId) {
                externalIdMap.put(externalId.getBridgeExternalIdKey(), externalId.getBridgeExternalIdValue());
            }
        }
        try {
            bridge.setExternalIds(ImmutableMap.copyOf(externalIdMap));
        } catch (NullPointerException e) {
            LOG.warn("Incomplete bridge external Id", e);
        }
    }



    private void setOpenDaylightOtherConfig(Bridge bridge, OvsdbBridgeAugmentation ovsdbManagedNode) {
        List<BridgeOtherConfigs> bridgeOtherConfig = ovsdbManagedNode.getBridgeOtherConfigs();
        if (bridgeOtherConfig != null) {
            Map<String, String> otherConfigMap = new HashMap<String, String>();
            for (BridgeOtherConfigs otherConf : bridgeOtherConfig) {
                otherConfigMap.put(otherConf.getBridgeOtherConfigKey(), otherConf.getBridgeOtherConfigValue());
            }
            try {
                bridge.setOtherConfig(ImmutableMap.copyOf(otherConfigMap));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete bridge other config");
            }
        }
    }



    private void setPort(TransactionBuilder transaction, Bridge bridge,
            OvsdbBridgeAugmentation ovsdbManagedNode) {

        Insert<GenericTableSchema> interfaceInsert = setInterface(transaction,ovsdbManagedNode);
        // Port part
        String portNamedUuid = "Port_" + SouthboundMapper.getRandomUUID();
        Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
        port.setName(ovsdbManagedNode.getBridgeName().getValue());
        port.setInterfaces(Sets.newHashSet(TransactUtils.extractNamedUuid(interfaceInsert)));
        transaction.add(op.insert(port).withId(portNamedUuid));
        bridge.setPorts(Sets.newHashSet(new UUID(portNamedUuid)));
    }

    private Insert<GenericTableSchema> setInterface(TransactionBuilder transaction,
            OvsdbBridgeAugmentation ovsdbManagedNode) {
        // Interface part
        String interfaceNamedUuid = "Interface_" + SouthboundMapper.getRandomUUID();
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
            String bridgeName) {
        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
        bridge.setName(bridgeName);
        bridge.setExternalIds(Collections.<String,String>emptyMap());
        Mutate mutate = TransactUtils.stampInstanceIdentifierMutation(transaction,
                iid,
                bridge.getSchema(),
                bridge.getExternalIdsColumn().getSchema());
        transaction.add(mutate
                .where(bridge.getNameColumn().getSchema().opEqual(bridgeName))
                .build());
    }

}