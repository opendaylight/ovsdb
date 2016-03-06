/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;

import org.opendaylight.ovsdb.lib.notation.Mutation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class MutationSerializer extends JsonSerializer<Mutation> {
    @Override
    public void serialize(Mutation condition, JsonGenerator generator,
        SerializerProvider provider) throws IOException {
        generator.writeStartArray();
        generator.writeString(condition.getColumn());
        generator.writeString(condition.getMutator().toString());
        generator.writeObject(condition.getValue());
        generator.writeEndArray();
    }
}