/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.junit.Test;
import org.opendaylight.of.lib.DecodeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opendaylight.of.lib.ext.nicira.NiciraActionSubtype.*;
import static org.opendaylight.util.junit.TestTools.*;

/**
 * Unit tests for {@link org.opendaylight.of.lib.ext.nicira.NiciraActionSubtype}.
 *
 * @author Simon Hunt
 */
public class NiciraActionSubtypeTest {

    @Test
    public void basic() {
        print(EOL + "basic()");
        for (NiciraActionSubtype nx: NiciraActionSubtype.values())
            print(nx);
        assertEquals(AM_UXS, 29, NiciraActionSubtype.values().length);
    }

    private void checkGood(NiciraActionSubtype nx, int code) {
        int encoded = nx.getCode();
        NiciraActionSubtype decoded = null;
        try {
            decoded = NiciraActionSubtype.decode(code);
        } catch (DecodeException e) {
            fail("Failed to decode: " + code);
        }
        print("{} code() => {}; {} decode() => {}", nx, encoded, code, decoded);
        assertEquals("unexpected code for const", code, encoded);
        assertEquals("unexpected const for code", nx, decoded);
    }

    @Test
    public void goodCodes() {
        print(EOL + "goodCodes()");
        checkGood(RESUBMIT, 1);
        checkGood(SET_TUNNEL, 2);
        checkGood(SET_QUEUE, 4);
        checkGood(POP_QUEUE, 5);
        checkGood(REG_MOVE, 6);
        checkGood(REG_LOAD, 7);
        checkGood(NOTE, 8);
        checkGood(SET_TUNNEL64, 9);
        checkGood(MULTIPATH, 10);
        checkGood(BUNDLE, 12);
        checkGood(BUNDLE_LOAD, 13);
        checkGood(RESUBMIT_TABLE, 14);
        checkGood(OUTPUT_REG, 15);
        checkGood(LEARN, 16);
        checkGood(EXIT, 17);
        checkGood(DEC_TTL, 18);
        checkGood(FIN_TIMEOUT, 19);
        checkGood(CONTROLLER, 20);
        checkGood(DEC_TTL_CNT_IDS, 21);
        checkGood(WRITE_METADATA, 22);
        checkGood(PUSH_MPLS, 23);
        checkGood(POP_MPLS, 24);
        checkGood(SET_MPLS_TTL, 25);
        checkGood(DEC_MPLS_TTL, 26);
        checkGood(STACK_PUSH, 27);
        checkGood(STACK_POP, 28);
        checkGood(SAMPLE, 29);
        checkGood(SET_MPLS_LABEL, 30);
        checkGood(SET_MPLS_TC, 31);
    }

    private void checkBad(int code) {
        try {
            NiciraActionSubtype decoded = NiciraActionSubtype.decode(code);
            fail(AM_NOEX + " for code: " + code);
        } catch (DecodeException e) {
            print("EX> {}", e);
        }
    }

    @Test
    public void badCodes() {
        print(EOL + "badCodes()");
        checkBad(-1);
        checkBad(0);
        checkBad(3);
        checkBad(11);
        checkBad(32);
    }

}
