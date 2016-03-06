/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.error;

/**
 * InvalidEncodingException in cases where something is not UTF-8 Encoded.
 */
public class InvalidEncodingException extends RuntimeException {

    private final String actual;

    public InvalidEncodingException(String actual, String message) {
        super(message);
        this.actual = actual;
    }

    public String getActual() {
        return actual;
    }
}
