/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.UUID;

public  class OvsdbTypesIdResolver extends TypeIdResolverBase {

    @Override
    public void init(JavaType bt) {
    }

    @Override
    public String idFromValue(Object value) {
        throw new UnsupportedOperationException("not yet done");
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        throw new UnsupportedOperationException("not yet done");
    }

    @Override
    public String idFromBaseType() {
        throw new UnsupportedOperationException("not yet done");
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        if ("set".equals(id)) {
            return context.getTypeFactory().constructCollectionType(OvsdbSet.class, Object.class);
        } else if ("uuid".equals(id) || "named-uuid".equals(id)) {
            return context.constructType(UUID.class);
        }
        return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        throw new UnsupportedOperationException("not yet done");
    }
}
