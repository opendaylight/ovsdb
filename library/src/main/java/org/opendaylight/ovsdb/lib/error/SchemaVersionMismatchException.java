/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.lib.error;

import org.opendaylight.ovsdb.lib.notation.Version;

/**
 * This exception is used when the a table or row is accessed though a typed interface
 * and the version requirements are not met
 */
public class SchemaVersionMismatchException extends RuntimeException {

    public SchemaVersionMismatchException(String message) {
        super(message);
    }

    public SchemaVersionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaVersionMismatchException(Version schemaVersion, Version fromVersion, Version untilVersion) {
        this("The schema version used to access the table/column (" + schemaVersion + ") does not match the required "
                + "version (from " + fromVersion + " to " + untilVersion + ")");
    }
}
