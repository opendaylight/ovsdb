/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerUpdateCommand.class);

    public ControllerUpdateCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<ControllerEntry>, ControllerEntry> controllers =
                TransactUtils.extractCreatedOrUpdated(getChanges(), ControllerEntry.class);
        Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> bridges =
                TransactUtils.extractCreatedOrUpdated(getChanges(), OvsdbBridgeAugmentation.class);
        LOG.info("execute: controllers: {} --- bridges: {}", controllers, bridges);
        for (Entry<InstanceIdentifier<ControllerEntry>, ControllerEntry> entry: controllers.entrySet()) {
            Optional<ControllerEntry> operationalControllerEntryOptional =
                    getOperationalState().getControllerEntry(entry.getKey());
            if (!operationalControllerEntryOptional.isPresent()) {
                InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                        entry.getKey().firstIdentifierOf(OvsdbBridgeAugmentation.class);
                Optional<OvsdbBridgeAugmentation> bridgeOptional =
                        getOperationalState().getOvsdbBridgeAugmentation(bridgeIid);
                OvsdbBridgeAugmentation ovsdbBridge = null;
                if (bridgeOptional.isPresent()) {
                    ovsdbBridge = bridgeOptional.get();
                } else {
                    ovsdbBridge = bridges.get(bridgeIid);
                }
                if (ovsdbBridge != null
                        && ovsdbBridge.getBridgeName() != null
                        && entry.getValue() != null
                        && entry.getValue().getTarget() != null) {
                    Controller controller =
                            TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Controller.class);
                    controller.setTarget(entry.getValue().getTarget().getValue());
                    String controllerNamedUuidString = SouthboundMapper.getRandomUUID();
                    UUID controllerNamedUuid = new UUID(controllerNamedUuidString);
                    transaction.add(op.insert(controller).withId(controllerNamedUuidString));

                    Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                    bridge.setName(ovsdbBridge.getBridgeName().getValue());
                    bridge.setController(Sets.newHashSet(controllerNamedUuid));
                    LOG.info("execute: bridge: {}", bridge);
                    transaction.add(op.mutate(bridge)
                            .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                    bridge.getControllerColumn().getData())
                            .where(bridge.getNameColumn().getSchema().opEqual(bridge.getNameColumn().getData()))
                            .build());
                }
            }
        }
        LOG.info("execute: transaction: {}", transaction.build());

    }

}
