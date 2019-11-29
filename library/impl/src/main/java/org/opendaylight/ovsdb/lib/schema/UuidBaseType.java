/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.notation.ReferencedRow;
import org.opendaylight.ovsdb.lib.notation.UUID;

final class UuidBaseType extends BaseType<UuidBaseType> {
    // These enum types correspond to JSON values and need to be in lower-case currently
    public enum RefType { strong, weak }

    static final UuidBaseType SINGLETON = new UuidBaseType();

    private String refTable;
    private UuidBaseType.RefType refType;

    @Override
    void fillConstraints(final JsonNode node) {
        JsonNode refTableNode = node.get("refTable");
        refTable = refTableNode != null ? refTableNode.asText() : null;

        JsonNode refTypeJson = node.get("refType");
        refType = refTypeJson != null ? RefType.valueOf(refTypeJson.asText()) : RefType.strong;
    }

    @Override
    public Object toValue(final JsonNode value) {
        if (value.isArray()) {
            if (value.size() == 2 && value.get(0).isTextual() && "uuid".equals(value.get(0).asText())) {
                return new UUID(value.get(1).asText());
            }
        } else {
            /*
             * UUIDBaseType used by RefTable from SouthBound will always be an Array of ["uuid", <uuid>].
             * But there are some cases from northbound where the RefTable type can be expanded to a Row
             * with contents. In those scenarios, just retain the content and return a ReferencedRow for
             * the upper layer functions to process it.
             */
            return new ReferencedRow(refTable, value);
        }
        return null;
    }

    @Override
    public void validate(final Object value) {

    }

    public String getRefTable() {
        return refTable;
    }

    public UuidBaseType.RefType getRefType() {
        return refType;
    }

    @Override
    public String toString() {
        return "UuidBaseType";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (refTable == null ? 0 : refTable.hashCode());
        result = prime * result
                + (refType == null ? 0 : refType.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UuidBaseType other = (UuidBaseType) obj;
        if (refTable == null) {
            if (other.refTable != null) {
                return false;
            }
        } else if (!refTable.equals(other.refTable)) {
            return false;
        }
        if (refType != other.refType) {
            return false;
        }
        return true;
    }
}