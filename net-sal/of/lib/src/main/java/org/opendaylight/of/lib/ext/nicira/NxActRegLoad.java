/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.opendaylight.of.lib.ProtocolVersion;
import org.opendaylight.util.PrimitiveUtils;

/**
 * Nicira extended action {@code REG_LOAD}.
 *
 * @author Simon Hunt
 */
public class NxActRegLoad extends NxAction {

/* Action structure for NXAST_REG_LOAD.
 *
 * Copies value[0:n_bits] to dst[ofs:ofs+n_bits], where a[b:c] denotes the bits
 * within 'a' numbered 'b' through 'c' (not including bit 'c').  Bit numbering
 * starts at 0 for the least-significant bit, 1 for the next most significant
 * bit, and so on.
 *
 * 'dst' is an nxm_header with nxm_hasmask=0.  See the documentation for
 * NXAST_REG_MOVE, above, for the permitted fields and for the side effects of
 * loading them.
 *
 * The 'ofs' and 'n_bits' fields are combined into a single 'ofs_nbits' field
 * to avoid enlarging the structure by another 8 bytes.  To allow 'n_bits' to
 * take a value between 1 and 64 (inclusive) while taking up only 6 bits, it is
 * also stored as one less than its true value:
 *
 *  15                           6 5                0
 * +------------------------------+------------------+
 * |              ofs             |    n_bits - 1    |
 * +------------------------------+------------------+
 *
 * The switch will reject actions for which ofs+n_bits is greater than the
 * width of 'dst', or in which any bits in 'value' with value 2**n_bits or
 * greater are set to 1, with error type OFPET_BAD_ACTION, code
 * OFPBAC_BAD_ARGUMENT.
 */
// struct nx_action_reg_load {
//     ovs_be16 type;                  /* OFPAT_VENDOR. */
//     ovs_be16 len;                   /* Length is 24. */
//     ovs_be32 vendor;                /* NX_VENDOR_ID. */
//     ovs_be16 subtype;               /* NXAST_REG_LOAD. */
//     ovs_be16 ofs_nbits;             /* (ofs << 6) | (n_bits - 1). */
//     ovs_be32 dst;                   /* Destination register. */
//     ovs_be64 value;                 /* Immediate value. */
// };
// OFP_ASSERT(sizeof(struct nx_action_reg_load) == 24);
    private static final int STRUCT_LEN = 24;

    int ofsNBits;
    long dst;
    long value;

    /**
     * Constructs the reg-load action.
     * @param pv protocol version
     * @param hdrlen the header length field
     */
    public NxActRegLoad(ProtocolVersion pv, int hdrlen) {
        // FIXME: we assume V_1_3 for now
        // use our own header instance
        super(NiciraActionSubtype.REG_LOAD);
        // copy the decoded header length
        setHeaderLength(hdrlen);
    }

    /**
     * Constructs the reg-load action.
     * Assumes V_1_3.
     */
    public NxActRegLoad() {
        this(ProtocolVersion.V_1_3, STRUCT_LEN);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        final int slen = sb.length();
        sb.replace(slen - 1, slen, "");
        // TODO: make this data more readable?
        sb.append(",ofs=").append(ofs())
                .append(",nbits=").append(nbits())
                .append(",dst=").append(hex(dst))
                .append(",value=").append(hex(value)).append('}');
        return sb.toString();
    }

    /**
     * Returns the value of ofs from the ofs_nbits field.
     *
     * @return the value of ofs
     */
    public int ofs() {
        return (ofsNBits & OFS_MASK) >> OFS_SHIFT;
    }

    private static final int OFS_MASK = 0xffc0;
    private static final int OFS_SHIFT = 6;

    /**
     * Returns the number of bits (nbits) from the ofs_nbits field. This will
     * be a value from 1 to 64.
     *
     * @return the number of bits
     */
    public int nbits() {
        // note, values 1..64 encoded as 0..63
        return (ofsNBits & NBITS_MASK) + 1;
    }

    private static final int NBITS_MASK = 0x3f;

    /**
     * Returns the destination register (u32).
     *
     * @return the destination register
     */
    public long dst() {
        return dst;
    }

    /**
     * Returns the immediate value (u64).
     * <p>
     * NOTE: spec defines this as u64, but we are returning in a long.
     * watch out for that sign bit!
     *
     * @return the immediate value
     */
    public long value() {
        return value;
    }

    // package private setters...

    /**
     * Sets the ofs and nbits values into the ofs_nbits field. Throws an
     * exception if either parameter is out of bounds:
     * <pre>
     *     ofs   : 0 .. 1023
     *     nbits : 1 .. 64
     * </pre>
     *
     * @param ofs the value to set ofs
     * @param nbits the number of bits
     * @throws IllegalArgumentException if either parameter is out of bounds
     */
    void setOfsNbits(int ofs, int nbits) {
        if (ofs < OFS_MIN || ofs > OFS_MAX)
            throw new IllegalArgumentException(E_OOB_OFS + ofs);
        if (nbits < NBITS_MIN || nbits > NBITS_MAX)
            throw new IllegalArgumentException(E_OOB_NBITS + nbits);

        ofsNBits = (ofs << OFS_SHIFT) | (nbits - 1);
    }

    private static final int OFS_MIN = 0;
    private static final int OFS_MAX = 1023;
    private static final int NBITS_MIN = 1;
    private static final int NBITS_MAX = 64;

    private static final String E_OOB_OFS = "OFS out of bounds: ";
    private static final String E_OOB_NBITS = "NBITS out of bounds: ";

    /**
     * Sets the destination register value.
     *
     * @param dst the destination register (u32)
     * @throws IllegalArgumentException if dst is not u32
     */
    void setDst(long dst) {
        PrimitiveUtils.verifyU32(dst);
        this.dst = dst;
    }

    /**
     * Sets the value.
     *
     * @param value the value
     */
    void setValue(long value) {
        this.value = value;
    }
}
