/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;

public interface TransactionCommand {

    void execute(ReadWriteTransaction transaction);

    default void onSuccess() {
    }

    default void onFailure() {
    }

    /**
     * Sets the result future of the executed/submitted transaction.
     */
    default void setTransactionResultFuture(FluentFuture future) {
    }

    default FluentFuture getTransactionResultFuture() {
        return null;
    }
}
