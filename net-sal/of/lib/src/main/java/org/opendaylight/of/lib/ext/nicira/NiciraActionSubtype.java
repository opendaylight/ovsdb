/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.opendaylight.of.lib.DecodeException;

/**
 * Designates the action subtypes as defined by the Nicira extentions.
 *
 * @author Simon Hunt
 */
public enum NiciraActionSubtype {
//    SNAT(0),              // Obsolete

    RESUBMIT(1),
    SET_TUNNEL(2),
//    DROP_SPOOFED_ARP(3),  // Obsolete
    SET_QUEUE(4),
    POP_QUEUE(5),
    REG_MOVE(6),
    REG_LOAD(7),
    NOTE(8),
    SET_TUNNEL64(9),
    MULTIPATH(10),
//    AUTOPATH(11),         // Obsolete
    BUNDLE(12),
    BUNDLE_LOAD(13),
    RESUBMIT_TABLE(14),
    OUTPUT_REG(15),
    LEARN(16),
    EXIT(17),
    DEC_TTL(18),
    FIN_TIMEOUT(19),
    CONTROLLER(20),
    DEC_TTL_CNT_IDS(21),
    WRITE_METADATA(22),
    PUSH_MPLS(23),
    POP_MPLS(24),
    SET_MPLS_TTL(25),
    DEC_MPLS_TTL(26),
    STACK_PUSH(27),
    STACK_POP(28),
    SAMPLE(29),
    SET_MPLS_LABEL(30),
    SET_MPLS_TC(31),
    ;

    private final int code;

    NiciraActionSubtype(int code) {
        this.code = code;
    }

    /**
     * Returns the code value for this constant.
     *
     * @return the code value
     */
    public int getCode() {
        return code;
    }

    /**
     * Decodes the given code and returns the appropriate subtype constant.
     *
     * @param code the encoded subtype
     * @return the corresponding subtype constant
     * @throws DecodeException if the code is unrecognized
     */
    static NiciraActionSubtype decode(int code) throws DecodeException {
        NiciraActionSubtype nx = null;
        for (NiciraActionSubtype n: values())
            if (n.code == code) {
                nx = n;
                break;
            }
        if (nx == null)
            throw new DecodeException("NiciraActionSubtype: code unknown: " + code);
        return nx;
    }
}
