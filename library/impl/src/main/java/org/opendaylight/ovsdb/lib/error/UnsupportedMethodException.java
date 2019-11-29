/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.error;

public class UnsupportedMethodException extends RuntimeException {
    private static final long serialVersionUID = -1665779125782132104L;

    public UnsupportedMethodException(final String message) {
        super(message);
    }

    public UnsupportedMethodException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
