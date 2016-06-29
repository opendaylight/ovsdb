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
import java.util.Collection;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;

public class RowSerializer extends JsonSerializer<Row> {
    @Override
    public void serialize(Row row, JsonGenerator generator,
        SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        Collection<Column> columns = row.getColumns();
        for (Column<?,?> column : columns) {
            generator.writeObjectField(column.getSchema().getName(), column.getData());
        }
        generator.writeEndObject();
    }
}
