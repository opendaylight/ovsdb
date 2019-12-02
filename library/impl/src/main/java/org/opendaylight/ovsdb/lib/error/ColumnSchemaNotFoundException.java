/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.error;

/**
 * This exception is thrown when a ColumnSchema cannot be found.
 */
public class ColumnSchemaNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -5273616784432907818L;

    public ColumnSchemaNotFoundException(final String message) {
        super(message);
    }

    public ColumnSchemaNotFoundException(final String columnName, final String tableName) {
        this("Unable to locate ColumnSchema for " +  columnName + " in " + tableName);
    }

    public ColumnSchemaNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
