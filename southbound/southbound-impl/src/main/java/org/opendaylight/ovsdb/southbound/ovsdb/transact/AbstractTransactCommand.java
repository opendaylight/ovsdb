/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static java.util.Objects.requireNonNull;

import org.opendaylight.ovsdb.lib.operations.Operations;

abstract class AbstractTransactCommand implements TransactCommand {
    final Operations op;

    AbstractTransactCommand(final Operations op) {
        this.op = requireNonNull(op);
    }
}
