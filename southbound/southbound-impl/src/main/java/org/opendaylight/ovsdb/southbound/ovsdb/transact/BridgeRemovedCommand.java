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
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class BridgeRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeRemovedCommand.class);

    public BridgeRemovedCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Set<InstanceIdentifier<OvsdbBridgeAugmentation>> removed =
                TransactUtils.extractRemoved(getChanges(),OvsdbBridgeAugmentation.class);
        Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> originals
            = TransactUtils.extractOriginal(getChanges(),OvsdbBridgeAugmentation.class);
        for (InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbManagedNodeIid: removed) {
            LOG.info("Received request to delete ovsdb node {}",ovsdbManagedNodeIid);
            OvsdbBridgeAugmentation original = originals.get(ovsdbManagedNodeIid);
            Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class,null);
            Optional<OvsdbBridgeAugmentation> ovsdbAugmentationOptional = getOperationalState()
                    .getOvsdbBridgeAugmentation(ovsdbManagedNodeIid);
            if (ovsdbAugmentationOptional.isPresent() && ovsdbAugmentationOptional.get().getBridgeUuid() != null) {
                UUID bridgeUuid = new UUID(ovsdbAugmentationOptional.get().getBridgeUuid().getValue());
                OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                        OpenVSwitch.class,null);
                transaction.add(op.delete(bridge.getSchema())
                        .where(bridge.getUuidColumn().getSchema().opEqual(bridgeUuid)).build());
                transaction.add(op.comment("Bridge: Deleting " + original.getBridgeName()));
                transaction.add(op.mutate(ovs.getSchema())
                        .addMutation(ovs.getBridgesColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(bridgeUuid)));
                transaction.add(op.comment("Open_vSwitch: Mutating " + original.getBridgeName() + " " + bridgeUuid));
            } else {
                LOG.warn("Unable to delete bridge {} because it was not found in the operational store, "
                        + "and thus we cannot retrieve its UUID", ovsdbManagedNodeIid);
            }

        }
    }
}
