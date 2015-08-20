/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

import com.fasterxml.jackson.databind.JsonNode;

public class ReferencedRow {
    String refTable;
    JsonNode jsonNode;
    public ReferencedRow(String refTable, JsonNode jsonNode) {
        this.refTable = refTable;
        this.jsonNode = jsonNode;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    public String getRefTable() {
        return refTable;
    }
}
