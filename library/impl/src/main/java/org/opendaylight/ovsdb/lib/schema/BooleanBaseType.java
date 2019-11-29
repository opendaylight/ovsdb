/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;

final class BooleanBaseType extends BaseType {
    static final BooleanBaseType SINGLETON = new BooleanBaseType();

    @Override
    void fillConstraints(final JsonNode node) {
        //no op
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asBoolean();
    }

    @Override
    public void validate(final Object value) {

    }

    @Override
    public String toString() {
        return "BooleanBaseType";
    }
}