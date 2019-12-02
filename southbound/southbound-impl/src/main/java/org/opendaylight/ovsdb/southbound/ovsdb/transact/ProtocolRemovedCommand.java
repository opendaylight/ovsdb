/*
 * Copyright © 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
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

public class ProtocolRemovedCommand implements TransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolRemovedCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractRemoved(events, ProtocolEntry.class),
                TransactUtils.extractCreatedOrUpdatedOrRemoved(events, OvsdbBridgeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractRemoved(modifications, ProtocolEntry.class),
                TransactUtils.extractCreatedOrUpdatedOrRemoved(modifications, OvsdbBridgeAugmentation.class));
    }

    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Set<InstanceIdentifier<ProtocolEntry>> removed,
            final Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updatedBridges) {
        for (InstanceIdentifier<ProtocolEntry> protocolIid : removed) {
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    protocolIid.firstIdentifierOf(OvsdbBridgeAugmentation.class);
            OvsdbBridgeAugmentation ovsdbBridge = updatedBridges.get(bridgeIid);
            Optional<ProtocolEntry> protocolEntryOptional = state.getProtocolEntry(protocolIid);
            if (ovsdbBridge != null
                    && protocolEntryOptional.isPresent()) {
                ProtocolEntry protocolEntry = protocolEntryOptional.get();
                if (protocolEntry != null && protocolEntry.getProtocol() != null) {
                    Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
                    String protocolString = SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocolEntry.getProtocol());
                    if (protocolString != null) {
                        bridge.setProtocols(Collections.singleton(protocolString));
                        try {
                            transaction.add(op.mutate(bridge).addMutation(bridge.getProtocolsColumn().getSchema(),
                                    Mutator.DELETE,bridge.getProtocolsColumn().getData()));
                            LOG.info("Removed ProtocolEntry : {} for OVSDB Bridge : {} ",
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
