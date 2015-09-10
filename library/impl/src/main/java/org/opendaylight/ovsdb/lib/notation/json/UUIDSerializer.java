/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;

import org.opendaylight.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class UUIDSerializer extends JsonSerializer<UUID> {
    @Override
    public void serialize(UUID value, JsonGenerator generator,
        SerializerProvider provider) throws IOException {
        generator.writeStartArray();
        try {
            java.util.UUID.fromString(value.toString());
            generator.writeString("uuid");
        } catch (IllegalArgumentException ex) {
            generator.writeString("named-uuid");
        }
        generator.writeString(value.toString());
        generator.writeEndArray();
    }
}