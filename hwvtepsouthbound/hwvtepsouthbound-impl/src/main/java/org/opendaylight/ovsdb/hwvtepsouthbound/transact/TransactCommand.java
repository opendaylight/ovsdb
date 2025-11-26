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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.EntryObject;

public interface TransactCommand<T extends EntryObject<?, ?>> {

    void execute(TransactionBuilder transaction);

    default void onConfigUpdate(TransactionBuilder transaction, DataObjectIdentifier<Node> nodeIid, T data,
                                DataObjectIdentifier key, Object... extraData) {
    }

    default void doDeviceTransaction(TransactionBuilder transaction, DataObjectIdentifier<Node> nodeIid, T data,
                                     DataObjectIdentifier key, Object... extraData) {
    }

    default void onSuccess(TransactionBuilder deviceTransaction) {
    }

    default void onFailure(TransactionBuilder deviceTransaction) {
    }

    default boolean retry() {
        return false;
    }
}
