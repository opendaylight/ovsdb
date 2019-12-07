/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.storage.mdsal;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

/**
 * A component in an overall storage transaction.
 */
public abstract class TransactionComponent {
    private final @NonNull ImmutableSet<Class<? extends TypedBaseTable<?>>> inputTableTypes;

    @SafeVarargs
    protected TransactionComponent(final Class<? extends TypedBaseTable<?>>... inputTableTypes) {
        this.inputTableTypes = ImmutableSet.copyOf(inputTableTypes);
    }

    /**
     * Return the table types which act as input to this transaction component.
     *
     * @return Input table types
     */
    public final @NonNull ImmutableSet<Class<? extends TypedBaseTable<?>>> getInputTableTypes() {
        return inputTableTypes;
    }
}
