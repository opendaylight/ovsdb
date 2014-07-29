/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.opendaylight.of.lib.*;
import org.opendaylight.of.lib.ext.ExtActionFactory;
import org.opendaylight.of.lib.instr.ActExperimenter;
import org.opendaylight.of.lib.instr.Action;

/**
 * Provides facilities for parsing, creating and encoding Nicira Extension
 * Action instances.
 *
 * @author Simon Hunt
 */
public class NiciraActionFactory extends AbstractFactory
        implements ExtActionFactory {

    private static final String E_UNEX_SUBTYPE = "Unexpected NX action subtype: ";

    /** Base length of a nicira action. */
    public static final int ACT_HEADER_LEN = 16;

    /** Singleton instance of the factory. */
    public static final NiciraActionFactory INSTANCE = new NiciraActionFactory();


    // No instantiation but here
    private NiciraActionFactory() {}

    /**
     * Returns an identifying tag for the factory.
     *
     * @return an identifying tag
     */
    @Override
    protected String tag() {
        return "NxAF";
    }

    // =======================================================================
    // ExtActionFactory API

    @Override
    public Action createActionInstance(ProtocolVersion pv, Action.Header header,
                                       int expId, OfPacketReader pkt)
            throws DecodeException {
        return NiciraActionParser.createActionInstance(pv, header, expId, pkt);
    }

    @Override
    public void encodeActionInstance(ActExperimenter act, OfPacketWriter pkt) {
        NiciraActionEncoder.encodeActionInstance(act, pkt);
    }

// =======================================================================
    // === Create Actions

    /**
     * Creates a REG_LOAD action.
     *
     * @param ofs the ofs (0..1023)
     * @param nbits the number of bits (1..64)
     * @param dst the destination register (u32)
     * @param value the value (u64)
     * @return the reg load action
     * @throws IllegalArgumentException if any parameter is out of bounds
     */
    public static NxAction createNxActionRegLoad(int ofs, int nbits, long dst,
                                                 long value) {
        NxActRegLoad act = new NxActRegLoad();
        act.setOfsNbits(ofs, nbits);
        act.setDst(dst);
        act.setValue(value);
        return act;
    }
}
