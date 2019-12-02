/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.error;

/**
 * This exception is thrown when a TableSchema cannot be found.
 */
public class TableSchemaNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -5030339562929850369L;

    public TableSchemaNotFoundException(final String message) {
        super(message);
    }

    public TableSchemaNotFoundException(final String tableName, final String schemaName) {
        this("Unable to locate TableSchema for " +  tableName + " in " + schemaName);
    }

    public TableSchemaNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
