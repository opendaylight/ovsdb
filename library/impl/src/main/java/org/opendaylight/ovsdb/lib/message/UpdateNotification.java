/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.opendaylight.ovsdb.lib.notation.json.Converter.UpdateNotificationConverter;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonDeserialize(converter = UpdateNotificationConverter.class)
public class UpdateNotification {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateNotification.class);

    private Object context;
    private DatabaseSchema databaseSchema;
    private TableUpdates update;
    private JsonNode updatesJson;

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public TableUpdates getUpdate() {
        return update;
    }

    public void setUpdate(TableUpdates update) {
        this.update = update;
    }

    @JsonAnySetter
    public void setValue(String key, JsonNode val) {
        LOG.error("setValue: Unexpected JSON property caught by @JsonAnySetter: key = {}, value = {} ", key, val);
    }

    public void setUpdates(JsonNode jsonNode) {
        this.updatesJson = jsonNode;
    }

    public JsonNode getUpdates() {
        return updatesJson;
    }

    public DatabaseSchema getDatabaseSchema() {
        return databaseSchema;
    }

    public void setDatabaseSchema(DatabaseSchema databaseSchema) {
        this.databaseSchema = databaseSchema;
    }
}
