/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation.json;

import com.fasterxml.jackson.databind.util.StdConverter;

import org.opendaylight.ovsdb.lib.notation.UUID;

public class UUIDStringConverter extends StdConverter<String, UUID> {

    @Override
    public UUID convert(String value) {
        return new UUID(value);
    }

}
