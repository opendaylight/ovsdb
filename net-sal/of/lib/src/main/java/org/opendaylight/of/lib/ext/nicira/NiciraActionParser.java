/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.opendaylight.of.lib.DecodeException;
import org.opendaylight.of.lib.OfPacketReader;
import org.opendaylight.of.lib.ProtocolVersion;
import org.opendaylight.of.lib.instr.Action;

/**
 * Parses Nicira actions.
 *
 * @author Simon Hunt
 */
public class NiciraActionParser {

    /**
     * Creates the appropriate (immutable) action instance from the given
     * parameters, and by completing the parsing of the data in the buffer.
     *
     * @param pv protocol version
     * @param header default action header
     * @param expId experimenter ID
     * @param pkt partially read buffer
     * @return the appropriate action instance
     */
    static Action createActionInstance(ProtocolVersion pv, Action.Header header,
                                       int expId, OfPacketReader pkt)
            throws DecodeException {
        int nxSubType = pkt.readU16();
        NiciraActionSubtype type = NiciraActionSubtype.decode(nxSubType);
        NxAction act = null;
        switch (type) {

            case RESUBMIT:
                break;
            case SET_TUNNEL:
                break;
            case SET_QUEUE:
                break;
            case POP_QUEUE:
                break;
            case REG_MOVE:
                break;
            case REG_LOAD:
                act = parseRegLoad(new NxActRegLoad(pv, header.length()), pkt);
                break;
            case NOTE:
                break;
            case SET_TUNNEL64:
                break;
            case MULTIPATH:
                break;
            case BUNDLE:
                break;
            case BUNDLE_LOAD:
                break;
            case RESUBMIT_TABLE:
                break;
            case OUTPUT_REG:
                break;
            case LEARN:
                break;
            case EXIT:
                break;
            case DEC_TTL:
                break;
            case FIN_TIMEOUT:
                break;
            case CONTROLLER:
                break;
            case DEC_TTL_CNT_IDS:
                break;
            case WRITE_METADATA:
                break;
            case PUSH_MPLS:
                break;
            case POP_MPLS:
                break;
            case SET_MPLS_TTL:
                break;
            case DEC_MPLS_TTL:
                break;
            case STACK_PUSH:
                break;
            case STACK_POP:
                break;
            case SAMPLE:
                break;
            case SET_MPLS_LABEL:
                break;
            case SET_MPLS_TC:
                break;
            default:
                // for now, just leave as null return
                break;
        }

        return act;
    }

    // ======================================================================
    // === Individual parsing methods

    // parses the remainder of the REG_LOAD action
    private static NxAction parseRegLoad(NxActRegLoad act, OfPacketReader pkt) {
        act.ofsNBits = pkt.readU16();
        act.dst = pkt.readU32();
        act.value = pkt.readLong();
        return act;
    }
}
