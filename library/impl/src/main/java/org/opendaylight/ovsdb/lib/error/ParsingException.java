/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.error;

/**
 * The ParsingException is thrown when JSON could not be successfully parsed.
 */
public class ParsingException extends RuntimeException {
    private static final long serialVersionUID = -3950575478467855764L;

    public ParsingException() {
    }

    public ParsingException(final String message) {
        super(message);
    }

    public ParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ParsingException(final Throwable cause) {
        super(cause);
    }

    public ParsingException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
