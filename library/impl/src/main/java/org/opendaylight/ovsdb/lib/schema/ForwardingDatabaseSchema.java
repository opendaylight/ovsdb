/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.google.common.collect.ForwardingObject;
import java.util.Set;
import org.opendaylight.ovsdb.lib.notation.Version;

public abstract class ForwardingDatabaseSchema extends ForwardingObject implements DatabaseSchema {
    @Override
    protected abstract DatabaseSchema delegate();

    @Override
    public Set<String> getTables() {
        return delegate().getTables();
    }

    @Override
    public boolean hasTable(final String table) {
        return delegate().hasTable(table);
    }

    @Override
    public <E extends TableSchema<E>> E table(final String tableName, final Class<E> clazz) {
        return delegate().table(tableName, clazz);
    }

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public Version getVersion() {
        return delegate().getVersion();
    }
}
