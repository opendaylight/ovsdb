/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Set;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;

public class OvsdbSetSerializer extends JsonSerializer<OvsdbSet<?>> {
    @Override
    public void serialize(OvsdbSet<?> set, JsonGenerator generator,
        SerializerProvider provider) throws IOException {
        generator.writeStartArray();
        generator.writeString("set");
        generator.writeStartArray();
        Set<?> javaSet = set.delegate();
        for (Object setObject : javaSet) {
            generator.writeObject(setObject);
        }
        generator.writeEndArray();
        generator.writeEndArray();
    }
}