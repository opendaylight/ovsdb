/*
 * (c) Copyright 2012,2013 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib;

/**
 * Exception thrown when an OpenFlow message fails to parse correctly.
 *
 * @author Simon Hunt
 */
public class MessageParseException extends OpenflowException {

    private static final long serialVersionUID = 6978750780580747194L;

    /** Constructs a new message parse exception with null as its detail
     *  message, and no specific cause.
     */
    public MessageParseException() {
        super();
    }

    /** Constructs a new message parse exception with the specified detail
     *  message, and the given cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public MessageParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Constructs a new message parse exception with the specified detail
     *  message, and no specific cause.
     *
     * @param message the detail message
     */
    public MessageParseException(String message) {
        super(message);
    }

    /** Constructs a new message parse exception with null as its detail
     *  message, but with the given cause.
     *
     * @param cause the underlying cause
     */
    public MessageParseException(Throwable cause) {
        super(cause);
    }
}
