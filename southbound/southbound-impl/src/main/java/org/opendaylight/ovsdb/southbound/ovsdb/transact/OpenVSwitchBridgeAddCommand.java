/*
 * Copyright © 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class OpenVSwitchBridgeAddCommand extends AbstractTransactCommand {
    public OpenVSwitchBridgeAddCommand(final Operations op) {
        super(op);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(op, transaction);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(op, transaction);
    }

    private static void execute(final Operations op, final TransactionBuilder transaction) {
        Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
        List<Insert> inserts = TransactUtils.extractInsert(transaction, bridge.getSchema());
        for (Insert insert : inserts) {
            OpenVSwitch ovs = transaction.getTypedRowWrapper(OpenVSwitch.class);
            ovs.setBridges(Collections.singleton(TransactUtils.extractNamedUuid(insert)));
            transaction.add(op.mutate(ovs).addMutation(ovs.getBridgesColumn().getSchema(),
                    Mutator.INSERT,
                    ovs.getBridgesColumn().getData()));
        }
    }
}
