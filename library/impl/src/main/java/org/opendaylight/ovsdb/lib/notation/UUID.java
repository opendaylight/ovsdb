/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

import org.opendaylight.ovsdb.lib.notation.json.UUIDSerializer;
import org.opendaylight.ovsdb.lib.notation.json.UUIDStringConverter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(contentConverter = UUIDStringConverter.class)
@JsonSerialize(using = UUIDSerializer.class)
/*
 * Handles both uuid and named-uuid.
 */
public class UUID {
    String val;

    public UUID(String value) {
        this.val = value;
    }

    @Override
    public String toString() {
        return val;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((val == null) ? 0 : val.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UUID other = (UUID) obj;
        if (val == null) {
            if (other.val != null) {
                return false;
            }
        } else if (!val.equals(other.val)) {
            return false;
        }
        return true;
    }
}
