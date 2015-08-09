/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.error;

/**
 * This exception is thrown when a ColumnSchema cannot be found
 */
public class ColumnSchemaNotFoundException extends RuntimeException {

    public ColumnSchemaNotFoundException(String message) {
        super(message);
    }

    public ColumnSchemaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static String createMessage(String columnName, String tableName) {
        String message = "Unable to locate ColumnSchema for " +  columnName + " in " + tableName;
        return message;
    }

}
