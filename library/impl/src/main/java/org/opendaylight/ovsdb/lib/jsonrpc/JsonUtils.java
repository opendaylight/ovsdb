/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public final class JsonUtils {
    private static final ObjectWriter PRETTY_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private JsonUtils() {
        // Hidden on purpose
    }

    public static String prettyString(final Object jsonNode) {
        try {
            return PRETTY_WRITER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
