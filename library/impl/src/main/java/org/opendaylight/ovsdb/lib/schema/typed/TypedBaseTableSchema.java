/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.schema.typed.MethodDispatch.Invoker;
import org.opendaylight.yangtools.concepts.Immutable;

public final class TypedBaseTableSchema implements Immutable {
    private final @NonNull ImmutableMap<Method, Invoker> invokers;
    private final @NonNull String tableName;



}
