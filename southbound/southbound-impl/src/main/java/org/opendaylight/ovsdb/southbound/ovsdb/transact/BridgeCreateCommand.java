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
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class BridgeCreateCommand extends AbstractTransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeCreateCommand.class);

    public BridgeCreateCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }



    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> created =
                TransactUtils.extractCreated(getChanges(),OvsdbBridgeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> ovsdbManagedNodeEntry:
            created.entrySet()) {
            OvsdbBridgeAugmentation ovsdbManagedNode = ovsdbManagedNodeEntry.getValue();
            LOG.debug("Received request to create ovsdb bridge name: {} uuid: {}",
                        ovsdbManagedNode.getBridgeName(),
                        ovsdbManagedNode.getBridgeUuid());

            // Named UUIDs
            String bridgeNamedUuid = "Bridge_" + SouthboundMapper.getRandomUUID();
            String interfaceNamedUuid = "Interface_" + SouthboundMapper.getRandomUUID();
            String portNamedUuid = "Port_" + SouthboundMapper.getRandomUUID();

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
            bridge.setPorts(Sets.newHashSet(new UUID(portNamedUuid)));

            // Set the iid external_id
            Map<String,String> externalIds = new HashMap<String,String>();
            externalIds.put(SouthboundConstants.IID_EXTERNAL_ID_KEY,
                    SouthboundUtil.serializeInstanceIdentifier(ovsdbManagedNodeEntry.getKey()));
            bridge.setExternalIds(externalIds);

            transaction.add(op.insert(bridge).withId(bridgeNamedUuid));
        }
    }

}
