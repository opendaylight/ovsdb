/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;

abstract class BaseTypeFactory<T extends BaseType<T>> {

    abstract T create(JsonNode typeDefinition);

    abstract static class WithEnum<T extends BaseType<T>, N extends Number> extends BaseTypeFactory<T> {

        final ImmutableSet<N> parseEnums(final JsonNode node) {
            final List<N> tmp = new ArrayList<>();
            for (JsonNode enm : node.get(1)) {
                tmp.add(getEnumValue(enm));
            }
            return ImmutableSet.copyOf(tmp);
        }

        abstract N getEnumValue(JsonNode jsonEnum);
    }
}
