/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.error;

/**
 * This is a generic exception thrown by the Typed Schema utilities.
 */
public class TyperException extends RuntimeException {
    private static final long serialVersionUID = 5464754787320848910L;

    public TyperException(final String message) {
        super(message);
    }

    public TyperException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
