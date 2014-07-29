/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.of.lib.ext;

import org.opendaylight.of.lib.DecodeException;
import org.opendaylight.of.lib.OfPacketReader;
import org.opendaylight.of.lib.OfPacketWriter;
import org.opendaylight.of.lib.ProtocolVersion;
import org.opendaylight.of.lib.instr.ActExperimenter;
import org.opendaylight.of.lib.instr.Action;

/**
 * Action Factory extensions must implement this API.
 *
 * @author Simon Hunt
 */
public interface ExtActionFactory {

    /**
     * Creates an action instance from a partially read buffer.
     * <p>
     * Note that the action header instance may need to be replaced by a
     * subclass instance so that the parsing code has access to adjust the
     * length field, which is protected, not public.
     * <p>
     * Note also that the experimenter ID is provided, even though typically it
     * can be inferred from the context. This allows a single implementation
     * to support more than one ID.
     *
     * @param pv the protocol version
     * @param header the parsed action header
     * @param expIdCode the experimenter ID code
     * @param pkt the buffer holding the remaining data
     * @return an appropriate action instance
     * @throws DecodeException if there is a decode failure
     */
    Action createActionInstance(ProtocolVersion pv, Action.Header header,
                                int expIdCode, OfPacketReader pkt)
            throws DecodeException;

    /**
     * Completes the encoding of the specified action instance into the
     * given packet writer. Note that the supplied buffer will be partially
     * completed; It should be assumed that the action header
     * (type == EXPERIMENTER, length), and the experimenter ID have already
     * been written out at the time this method is invoked. The remaining
     * action-specific data should be written out to the buffer.
     *
     * @param act the action to encode
     * @param pkt the buffer into which the action must be encoded
     * @throws IllegalArgumentException if the given action class is not
     *          supported by this factory implementation
     */
    void encodeActionInstance(ActExperimenter act, OfPacketWriter pkt);
}
