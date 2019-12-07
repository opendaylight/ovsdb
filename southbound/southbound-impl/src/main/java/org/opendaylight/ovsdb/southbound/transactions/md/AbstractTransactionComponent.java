/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.storage.mdsal.TransactionComponent;

abstract class AbstractTransactionComponent extends TransactionComponent<OvsdbTransactionContext> {
    @SafeVarargs
    protected AbstractTransactionComponent(final Class<? extends TypedBaseTable<?>>... inputTableTypes) {
        super(inputTableTypes);
    }
}
