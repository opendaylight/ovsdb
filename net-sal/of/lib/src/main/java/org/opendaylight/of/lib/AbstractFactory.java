/*
 * (c) Copyright 2012,2013 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib;

/**
 * Provides a base implementation for all the factory classes out there.
 *
 * @author Simon Hunt
 */
public abstract class AbstractFactory {

    /** Returns a message parse exception with debugging information.
     *
     * @param pkt the packet buffer being parsed at the time of the exception
     * @param msg additional detail message (may be null)
     * @param e the causing exception (may be null)
     * @return a ready-to-throw MessageParseException
     */
    public MessageParseException mpe(OfPacketReader pkt, String msg,
                                        Exception e) {
        StringBuilder sb = new StringBuilder(tag()).append(":").append(pkt);
        if (msg != null)
            sb.append(" ").append(msg);
        if (e != null)
            sb.append(" > ").append(e);
        return new MessageParseException(sb.toString(), e);
    }

    /** Convenience method delegating to
     * {@link #mpe(OfPacketReader, String, Exception)}
     * with a null message.
     *
     * @param pkt the packet buffer being parsed at the time of the exception
     * @param e the causing exception (may be null)
     * @return a ready-to-throw MessageParseException
     */
    public MessageParseException mpe(OfPacketReader pkt, Exception e) {
        return mpe(pkt, null, e);
    }

    /** Convenience method delegating to
     * {@link #mpe(OfPacketReader, String, Exception)}
     * with a null exception.
     *
     * @param pkt the packet buffer being parsed at the time of the exception
     * @param msg additional detail message (may be null)
     * @return a ready-to-throw MessageParseException
     */
    public MessageParseException mpe(OfPacketReader pkt, String msg) {
        return mpe(pkt, msg, null);
    }

    /** Convenience method delegating to
     * {@link #mpe(OfPacketReader, String, Exception)}
     * with a null message and null exception.
     *
     * @param pkt the packet buffer being parsed at the time of the exception
     * @return a ready-to-throw MessageParseException
     */
    public MessageParseException mpe(OfPacketReader pkt) {
        return mpe(pkt, null, null);
    }

    /** Subclasses should return a short identifying string. This is used
     * in exception messages generated by {@link #mpe}.
     *
     * @return a short identifying string
     */
    protected abstract String tag();
}
