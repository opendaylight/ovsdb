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
import java.util.Map.Entry;

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

public class ProtocolUpdateCommand extends AbstractTransactCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolUpdateCommand.class);
    private Map<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> protocols;
    private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> bridges;

    public ProtocolUpdateCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
        protocols = TransactUtils.extractCreatedOrUpdated(getChanges(), ProtocolEntry.class);
        bridges = TransactUtils.extractCreatedOrUpdated(getChanges(), OvsdbBridgeAugmentation.class);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (Entry<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> entry: protocols.entrySet()) {
            Optional<ProtocolEntry> operationalProtocolEntryOptional =
                    getOperationalState().getProtocolEntry(entry.getKey());
            if (!operationalProtocolEntryOptional.isPresent()) {
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
                        && entry.getValue().getProtocol() != null) {
                    String protocolString = SouthboundConstants.OVSDB_PROTOCOL_MAP.get(entry.getValue().getProtocol());
                    if (protocolString != null) {
                        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                        bridge.setName(ovsdbBridge.getBridgeName().getValue());
                        try {
                            bridge.setProtocols(Sets.newHashSet(protocolString));
                            transaction.add(op.mutate(bridge).addMutation(bridge.getProtocolsColumn().getSchema(),
                                        Mutator.INSERT,bridge.getProtocolsColumn().getData())
                                .where(bridge.getNameColumn().getSchema().opEqual(bridge.getNameColumn().getData()))
                                .build());
                        } catch (SchemaVersionMismatchException e) {
                            LOG.warn("protocol not supported by this version of ovsdb", e);
                        }
                    }
                }
            }
        }
    }

}
