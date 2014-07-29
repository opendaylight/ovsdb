/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.of.lib.ext.nicira;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.of.lib.*;
import org.opendaylight.of.lib.instr.Action;
import org.opendaylight.of.lib.instr.ActionFactory;
import org.opendaylight.of.lib.instr.ActionType;

import static org.junit.Assert.*;
import static org.opendaylight.of.lib.ProtocolVersion.V_1_3;
import static org.opendaylight.util.ByteUtils.toHexArrayString;
import static org.opendaylight.util.junit.TestTools.*;

/**
 * Unit tests for the creation, encoding, and parsing of Nicira actions,
 * via the {@link org.opendaylight.of.lib.ext.nicira.NiciraActionFactory}.
 *
 * @author Simon Hunt
 */
public class NiciraActionFactoryTest extends AbstractTest {

    private static final String HEX_REG_LOAD_1 = "nxActionRegLoad1.hex";
    private static final String NXACT_PATH = "ext/nicira/";

    private Action act;
    private NxAction nxa;
    private OfPacketReader pkt;

    @BeforeClass
    public static void classSetup() {
        ActionFactory.registerExtension(ExperimenterId.NICIRA,
                NiciraActionFactory.INSTANCE);
    }

    @AfterClass
    public static void classTearDown() {
        ActionFactory.unregisterExtension(ExperimenterId.NICIRA);
    }

    private OfPacketReader getOfmTestReader(String dataFile) {
        return getPacketReader(NXACT_PATH + dataFile);
    }

    private Action parseActionHexFile(String dataFile) {
        pkt = getOfmTestReader(dataFile);
        try {
            act = ActionFactory.parseAction(pkt, V_1_3);
        } catch (MessageParseException e) {
            print(e);
            fail("Unexpected MPE: " + e);
        }
        return act;
    }

    private byte[] getExpectedBytes(String dataFile) {
        return slurpedBytes(NXACT_PATH + dataFile);
    }

    private void verifyEncodedAction(String filename, Action act) {
        byte[] expBytes = getExpectedBytes(filename);
        OfPacketWriter pkt = new OfPacketWriter(expBytes.length);
        ActionFactory.encodeAction(act, pkt);
        verifyEncodement(filename, expBytes, pkt);
    }

    private void verifyEncodement(String f, byte[] exp, OfPacketWriter pkt) {
        print("...verifying encodement for {}", f);
        assertEquals("buffer too big", 0, pkt.writableBytes());
        byte[] encoded = pkt.array();
        printHexArray(encoded);
        debugPrint(f, exp, encoded);
        assertArrayEquals(AM_NEQ, exp, encoded);
    }

    private void printHexArray(byte[] array) {
        StringBuilder sb = new StringBuilder(toHexArrayString(array));
        int len = sb.length();
        if (len > 102)
            sb.replace(100, len, "... ]");
        print("Encoded: [{}] {}", array.length, sb);
    }


    // ========================
    // === REG_LOAD

    // see nxActionRegLoad1.hex
    private static final int RL1_OFS = 13;
    private static final int RL1_NBITS = 49;
    private static final int RL1_DST = 0x1234;
    private static final long RL1_VALUE = 0xcafef00dL;

    @Test
    public void parseRegLoad1() {
        print(EOL + "parseRegLoad1()");
        act = parseActionHexFile(HEX_REG_LOAD_1);
        print(act);
        verifyRegLoad1();
    }

    @Test
    public void createRegLoad1() {
        print(EOL + "createRegLoad1()");
        act = NiciraActionFactory.createNxActionRegLoad(RL1_OFS, RL1_NBITS,
                RL1_DST, RL1_VALUE);
        print(act);
        verifyRegLoad1();
    }

    private void verifyRegLoad1() {
        // as an Action, we can assert the following...
        assertEquals(AM_NEQ, ActionType.EXPERIMENTER, act.getActionType());
        assertEquals(AM_UXS, 24, act.getTotalLength());

        assertTrue(AM_WRCL, act instanceof NxAction);
        nxa = (NxAction) act;

        // as an NxAction, we can assert the following...
        assertEquals(AM_NEQ, NiciraActionSubtype.REG_LOAD, nxa.getNxaSubtype());

        assertTrue(AM_WRCL, act instanceof NxActRegLoad);
        NxActRegLoad regLoad = (NxActRegLoad) act;

        // as an NxActRegLoad, we can assert the following...

        // NOTE the splitting of the raw ofs_nbits field into separate calls..
        //      reducing the burden on the consumer
        assertEquals(AM_NEQ, RL1_OFS, regLoad.ofs());
        assertEquals(AM_NEQ, RL1_NBITS, regLoad.nbits());

        assertEquals(AM_NEQ, RL1_DST, regLoad.dst());
        assertEquals(AM_NEQ, RL1_VALUE, regLoad.value());
    }

    @Test
    public void encodeRegLoad1() {
        print(EOL + "encodeRegLoad1()");
        act = NiciraActionFactory.createNxActionRegLoad(RL1_OFS, RL1_NBITS,
                RL1_DST, RL1_VALUE);
        verifyEncodedAction(HEX_REG_LOAD_1, act);
    }


}
