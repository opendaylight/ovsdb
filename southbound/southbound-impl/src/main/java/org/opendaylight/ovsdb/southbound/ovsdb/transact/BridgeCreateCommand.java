/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
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
        Map<InstanceIdentifier<Node>, OvsdbBridgeAugmentation> created = TransactUtils.extractOvsdbManagedNodeCreate(changes);
        for(OvsdbBridgeAugmentation omn: created.values()) {
            LOG.info("Received request to create ovsdb bridge {}",omn);
            // Bridge part
            Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
            bridge.setName(omn.getBridgeName().getValue());
            if(SouthboundMapper.createOvsdbBridgeProtocols(omn) != null
                    && SouthboundMapper.createOvsdbBridgeProtocols(omn).size() > 0){
                bridge.setProtocols(SouthboundMapper.createOvsdbBridgeProtocols(omn));
            }
            Map<UUID,Controller> controllerMap = SouthboundMapper.createOvsdbController(omn, transaction.getDatabaseSchema());
            for(Entry<UUID,Controller >entry: controllerMap.entrySet()) {
                transaction.add(op.insert(entry.getValue()).withId(entry.getKey().toString()));
            }
            if(!controllerMap.isEmpty()) {
                bridge.setController(controllerMap.keySet());
            }

            String bridgeNamedUuid = "Bridge_" + omn.getBridgeName().getValue();
            transaction.add(op.insert(bridge).withId(bridgeNamedUuid));

            // OpenVSwitchPart
            OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);
            ovs.setBridges(Sets.newHashSet(new UUID(bridgeNamedUuid)));
            transaction.add(op.mutate(ovs).addMutation(ovs.getBridgesColumn().getSchema(),
                    Mutator.INSERT,
                    ovs.getBridgesColumn().getData())
                    );
        }
    }

}
