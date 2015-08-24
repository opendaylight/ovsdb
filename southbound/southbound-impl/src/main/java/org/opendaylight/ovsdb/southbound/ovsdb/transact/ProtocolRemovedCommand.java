/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class ProtocolRemovedCommand extends AbstractTransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolRemovedCommand.class);
    private Set<InstanceIdentifier<ProtocolEntry>> removed;
    private Map<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> operationalProtocolEntries;
    private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updatedBridges;

    public ProtocolRemovedCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
        removed = TransactUtils.extractRemoved(getChanges(),ProtocolEntry.class);
        operationalProtocolEntries = TransactUtils.extractOriginal(getChanges(),ProtocolEntry.class);
        updatedBridges = TransactUtils.extractCreatedOrUpdatedOrRemoved(getChanges(),OvsdbBridgeAugmentation.class);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (InstanceIdentifier<ProtocolEntry> protocolIid : removed) {
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    protocolIid.firstIdentifierOf(OvsdbBridgeAugmentation.class);
            OvsdbBridgeAugmentation ovsdbBridge = updatedBridges.get(bridgeIid);
            Optional<ProtocolEntry> protocolEntryOptional = getOperationalState().getProtocolEntry(protocolIid);
            if (ovsdbBridge != null
                    && protocolEntryOptional.isPresent()) {
                ProtocolEntry protocolEntry = protocolEntryOptional.get();
                if (protocolEntry != null && protocolEntry.getProtocol() != null) {
                    Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                    String protocolString = SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocolEntry.getProtocol());
                    if (protocolString != null) {
                        bridge.setProtocols(Sets.newHashSet(protocolString));
                        try {
                            transaction.add(op.mutate(bridge).addMutation(bridge.getProtocolsColumn().getSchema(),
                                    Mutator.DELETE,bridge.getProtocolsColumn().getData()));
                        } catch (SchemaVersionMismatchException e) {
                            LOG.warn("protocol is not supported by this version of ovsdb", e);
                        }
                    }
                }
            }
        }

    }

}
