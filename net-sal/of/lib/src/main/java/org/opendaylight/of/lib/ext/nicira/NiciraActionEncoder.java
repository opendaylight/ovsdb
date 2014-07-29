/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.opendaylight.of.lib.OfPacketWriter;
import org.opendaylight.of.lib.instr.ActExperimenter;

/**
 * Encodes Nicira actions.
 *
 * @author Simon Hunt
 */
public class NiciraActionEncoder {

    private static final String E_UNSUPPORTED_CLASS =
            "Not an NxAction: ";

    /**
     * Writes the remaining action-specific data to the specified buffer.
     *
     * @param act the action instance to encode
     * @param pkt the buffer
     */
    static void encodeActionInstance(ActExperimenter act, OfPacketWriter pkt) {
        if (!(act instanceof NxAction))
            throw new IllegalArgumentException(E_UNSUPPORTED_CLASS +
                    act.getClass().getName());

        NxAction nxa = (NxAction) act;
        NiciraActionSubtype type = nxa.getNxaSubtype();
        // we can write out the subtype...
        pkt.writeU16(type.getCode());

        // but the remaining payload depends on the action type...
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
                encodeRegLoad((NxActRegLoad) nxa, pkt);
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
        }
    }

    // ======================================================================
    // === Individual encoding methods

    // encodes the remainder of the REG_LOAD action
    private static void encodeRegLoad(NxActRegLoad nxa, OfPacketWriter pkt) {
        pkt.writeU16(nxa.ofsNBits);
        pkt.writeU32(nxa.dst);
        pkt.writeLong(nxa.value);
    }

}
