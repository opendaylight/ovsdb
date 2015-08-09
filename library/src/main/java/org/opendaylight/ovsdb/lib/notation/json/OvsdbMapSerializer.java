/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.OvsdbMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OvsdbMapSerializer extends JsonSerializer<OvsdbMap<?,?>> {
    @Override
    public void serialize(OvsdbMap<?,?> map, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();
        generator.writeString("map");
        generator.writeStartArray();
        Map<?,?> javaMap = map.delegate();
        for (Object set : javaMap.keySet()) {
            generator.writeStartArray();
            generator.writeObject(set);
            generator.writeObject(javaMap.get(set));
            generator.writeEndArray();
        }
        generator.writeEndArray();
        generator.writeEndArray();
    }
}