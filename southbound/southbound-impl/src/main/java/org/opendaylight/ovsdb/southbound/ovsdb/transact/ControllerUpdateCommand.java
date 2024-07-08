/*
 * Copyright © 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerUpdateCommand.class);

    public ControllerUpdateCommand(final Operations op) {
        super(op);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(op, transaction, state, TransactUtils.extractCreatedOrUpdated(events, ControllerEntry.class),
                TransactUtils.extractCreatedOrUpdated(events, OvsdbBridgeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(op, transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, ControllerEntry.class),
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbBridgeAugmentation.class));
    }

    private static void execute(final Operations op, final TransactionBuilder transaction,
            final BridgeOperationalState state,
            final Map<InstanceIdentifier<ControllerEntry>, ControllerEntry> controllers,
            final Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> bridges) {
        LOG.info("Register ODL controllers : {}  bridges detail : {}",
                controllers, bridges);
        for (Entry<InstanceIdentifier<ControllerEntry>, ControllerEntry> entry: controllers.entrySet()) {
            Optional<ControllerEntry> operationalControllerEntryOptional =
                    state.getControllerEntry(entry.getKey());
            if (!operationalControllerEntryOptional.isPresent()) {
                InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                        entry.getKey().firstIdentifierOf(OvsdbBridgeAugmentation.class);
                Optional<OvsdbBridgeAugmentation> bridgeOptional =
                        state.getOvsdbBridgeAugmentation(bridgeIid);
                OvsdbBridgeAugmentation ovsdbBridge = bridgeOptional.isPresent()
                    ? bridgeOptional.orElseThrow() : bridges.get(bridgeIid);
                if (ovsdbBridge != null
                        && ovsdbBridge.getBridgeName() != null
                        && entry.getValue() != null
                        && entry.getValue().getTarget() != null) {
                    ControllerEntry controllerEntry = entry.getValue();
                    Controller controller = transaction.getTypedRowWrapper(Controller.class);
                    controller.setTarget(controllerEntry.getTarget().getValue());
                    if (controllerEntry.getMaxBackoff() != null) {
                        controller.setMaxBackoff(Collections.singleton(controllerEntry.getMaxBackoff().toJava()));
                    }
                    if (controllerEntry.getInactivityProbe() != null) {
                        controller.setInactivityProbe(Collections.singleton(
                            controllerEntry.getInactivityProbe().toJava()));
                    }
                    String controllerNamedUuidString = SouthboundMapper.getRandomUuid();
                    UUID controllerNamedUuid = new UUID(controllerNamedUuidString);
                    transaction.add(op.insert(controller).withId(controllerNamedUuidString));

                    Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
                    bridge.setName(ovsdbBridge.getBridgeName().getValue());
                    bridge.setController(Collections.singleton(controllerNamedUuid));
                    LOG.trace("Added controller : {} for bridge : {}",
                            controller.getTargetColumn(), bridge.getName());
                    transaction.add(op.mutate(bridge)
                            .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                    bridge.getControllerColumn().getData())
                            .where(bridge.getNameColumn().getSchema().opEqual(bridge.getNameColumn().getData()))
                            .build());
                }
            }
        }
        LOG.trace("Executed transaction: {}", transaction.build());

    }
}
