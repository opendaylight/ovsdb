/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.error;

/**
 * BadSchema exception is thrown when the received schema is invalid.
 */
public class BadSchemaException extends RuntimeException {
    private static final long serialVersionUID = -7045398620135011253L;

    public BadSchemaException(final String message) {
        super(message);
    }

    public BadSchemaException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
