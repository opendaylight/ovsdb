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
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class ControllerRemovedCommand extends AbstractTransactCommand {

    public ControllerRemovedCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Set<InstanceIdentifier<ControllerEntry>> removed =
                TransactUtils.extractRemoved(getChanges(),ControllerEntry.class);
        Map<InstanceIdentifier<ControllerEntry>, ControllerEntry> operationalControllerEntries
            = TransactUtils.extractOriginal(getChanges(),ControllerEntry.class);
        Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updatedBridges
            = TransactUtils.extractCreatedOrUpdatedOrRemoved(getChanges(),OvsdbBridgeAugmentation.class);
        for (InstanceIdentifier<ControllerEntry> controllerIid : removed) {
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    controllerIid.firstIdentifierOf(OvsdbBridgeAugmentation.class);
            OvsdbBridgeAugmentation ovsdbBridge = updatedBridges.get(bridgeIid);
            Optional<ControllerEntry> controllerEntryOptional = getOperationalState().getControllerEntry(controllerIid);
            if (ovsdbBridge != null && controllerEntryOptional.isPresent()) {
                ControllerEntry controllerEntry = controllerEntryOptional.get();
                Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                bridge.setController(Sets.newHashSet(new UUID(controllerEntry.getControllerUuid().getValue())));
                transaction.add(op.mutate(bridge).addMutation(bridge.getControllerColumn().getSchema(),
                        Mutator.DELETE, bridge.getControllerColumn().getData()));
            }
        }


    }

}
