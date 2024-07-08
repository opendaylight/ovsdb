/*
 * Copyright © 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolUpdateCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, ProtocolEntry.class),
                TransactUtils.extractCreatedOrUpdated(events, OvsdbBridgeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, ProtocolEntry.class),
                TransactUtils.extractCreatedOrUpdated(modifications, OvsdbBridgeAugmentation.class));
    }

    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> protocols,
            final Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> bridges) {
        for (Entry<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> entry: protocols.entrySet()) {
            Optional<ProtocolEntry> operationalProtocolEntryOptional =
                    state.getProtocolEntry(entry.getKey());
            if (!operationalProtocolEntryOptional.isPresent()) {
                InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                        entry.getKey().firstIdentifierOf(OvsdbBridgeAugmentation.class);
                Optional<OvsdbBridgeAugmentation> bridgeOptional =
                        state.getOvsdbBridgeAugmentation(bridgeIid);
                OvsdbBridgeAugmentation ovsdbBridge;
                if (bridgeOptional.isPresent()) {
                    ovsdbBridge = bridgeOptional.orElseThrow();
                } else {
                    ovsdbBridge = bridges.get(bridgeIid);
                }
                if (ovsdbBridge != null
                        && ovsdbBridge.getBridgeName() != null
                        && entry.getValue() != null
                        && entry.getValue().getProtocol() != null) {
                    String protocolString = SouthboundConstants.OVSDB_PROTOCOL_MAP.get(entry.getValue().getProtocol());
                    if (protocolString != null) {
                        Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
                        bridge.setName(ovsdbBridge.getBridgeName().getValue());
                        try {
                            bridge.setProtocols(Collections.singleton(protocolString));
                            transaction.add(op.mutate(bridge).addMutation(bridge.getProtocolsColumn().getSchema(),
                                        Mutator.INSERT,bridge.getProtocolsColumn().getData())
                                .where(bridge.getNameColumn().getSchema().opEqual(bridge.getNameColumn().getData()))
                                .build());
                            LOG.info("Updated ProtocolEntry : {} for OVSDB Bridge : {} ",
                                    protocolString, bridge.getName());
                        } catch (SchemaVersionMismatchException e) {
                            schemaMismatchLog("protocols", "Bridge", e);
                        }
                    }
                }
            }
        }
    }

}
