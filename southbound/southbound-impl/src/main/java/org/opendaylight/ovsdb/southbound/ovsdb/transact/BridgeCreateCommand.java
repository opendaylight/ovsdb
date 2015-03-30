/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class BridgeCreateCommand implements TransactCommand {
    private AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes;
    private static final Logger LOG = LoggerFactory.getLogger(BridgeCreateCommand.class);


    public BridgeCreateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes) {
        this.changes = changes;
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, OvsdbBridgeAugmentation> created =
                TransactUtils.extractOvsdbManagedNodeCreate(changes);
        for (Entry<InstanceIdentifier<Node>, OvsdbBridgeAugmentation> ovsdbManagedNodeEntry:
            created.entrySet()) {
            OvsdbBridgeAugmentation ovsdbManagedNode = ovsdbManagedNodeEntry.getValue();
            LOG.debug("Received request to create ovsdb bridge name: {} uuid: {}",
                        ovsdbManagedNode.getBridgeName(),
                        ovsdbManagedNode.getBridgeUuid());

            // Named UUIDs
            String bridgeNamedUuid = "Bridge_" + ovsdbManagedNode.getBridgeName().getValue();
            String interfaceNamedUuid = "Interface_" + ovsdbManagedNode.getBridgeName().getValue();
            String portNamedUuid = "Port_" + ovsdbManagedNode.getBridgeName().getValue();

            // Interface part
            Interface interfaceOvs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Interface.class);
            interfaceOvs.setName(ovsdbManagedNode.getBridgeName().getValue());
            interfaceOvs.setType(SouthboundMapper.createOvsdbInterfaceType(InterfaceTypeInternal.class));
            transaction.add(op.insert(interfaceOvs).withId(interfaceNamedUuid));

            // Port part
            Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class);
            port.setName(ovsdbManagedNode.getBridgeName().getValue());
            port.setInterfaces(Sets.newHashSet(new UUID(interfaceNamedUuid)));
            transaction.add(op.insert(port).withId(portNamedUuid));

            // Bridge part
            Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
            bridge.setName(ovsdbManagedNode.getBridgeName().getValue());
            if (ovsdbManagedNode.getFailMode() != null
                    && SouthboundConstants.OVSDB_FAIL_MODE_MAP.get(ovsdbManagedNode.getFailMode()) != null ) {
                bridge.setFailMode(Sets.newHashSet(
                        SouthboundConstants.OVSDB_FAIL_MODE_MAP.get(ovsdbManagedNode.getFailMode())));
            }
            bridge.setDatapathType(SouthboundMapper.createDatapathType(ovsdbManagedNode));
            if (SouthboundMapper.createOvsdbBridgeProtocols(ovsdbManagedNode) != null
                    && SouthboundMapper.createOvsdbBridgeProtocols(ovsdbManagedNode).size() > 0) {
                bridge.setProtocols(SouthboundMapper.createOvsdbBridgeProtocols(ovsdbManagedNode));
            }
            Map<UUID,Controller> controllerMap = SouthboundMapper.createOvsdbController(
                    ovsdbManagedNode, transaction.getDatabaseSchema());
            for (Entry<UUID,Controller> entry: controllerMap.entrySet()) {
                transaction.add(op.insert(entry.getValue()).withId(entry.getKey().toString()));
            }
            if (!controllerMap.isEmpty()) {
                bridge.setController(controllerMap.keySet());
            }
            bridge.setPorts(Sets.newHashSet(new UUID(portNamedUuid)));

            // Set the iid external_id
            Map<String,String> externalIds = new HashMap<String,String>();
            externalIds.put(SouthboundConstants.IID_EXTERNAL_ID_KEY,
                    SouthboundUtil.serializeInstanceIdentifier(ovsdbManagedNodeEntry.getKey()));
            bridge.setExternalIds(externalIds);

            transaction.add(op.insert(bridge).withId(bridgeNamedUuid));

            // OpenVSwitchPart
            OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);
            ovs.setBridges(Sets.newHashSet(new UUID(bridgeNamedUuid)));
            transaction.add(op.mutate(ovs).addMutation(ovs.getBridgesColumn().getSchema(),
                    Mutator.INSERT,
                    ovs.getBridgesColumn().getData()));
        }
    }

}
