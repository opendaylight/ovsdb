/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface TransactCommand<T extends Identifiable> {

    void execute(TransactionBuilder transaction);

    void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data);

    void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data);
}
