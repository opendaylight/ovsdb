/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.of.lib.AbstractTest;
import org.opendaylight.of.lib.ExperimenterId;
import org.opendaylight.of.lib.ProtocolVersion;
import org.opendaylight.of.lib.instr.ActionType;

import static junit.framework.Assert.assertEquals;
import static org.opendaylight.util.junit.TestTools.*;

/**
 * Unit tests for {@link NxActRegLoad}.
 *
 * @author Simon Hunt
 */
public class NxActRegLoadTest extends AbstractTest {

    private static final long MAX_U32 = (long)Integer.MAX_VALUE * 2 + 1;

    private NxActRegLoad regLoad;

    @Before
    public void setUp() {
        regLoad = new NxActRegLoad();
    }

    @Test
    public void basic() {
        print(EOL + "basic()");
        print(regLoad);
        assertEquals(AM_NEQ, ProtocolVersion.V_1_3, regLoad.getVersion());
        assertEquals(AM_NEQ, ActionType.EXPERIMENTER, regLoad.getActionType());
        assertEquals(AM_UXS, 24, regLoad.getTotalLength());
        assertEquals(AM_NEQ, ExperimenterId.NICIRA.encodedId(), regLoad.getId());
        assertEquals(AM_NEQ, NiciraActionSubtype.REG_LOAD, regLoad.getNxaSubtype());
        assertEquals(AM_NEQ, 0, regLoad.ofs());
        assertEquals(AM_NEQ, 1, regLoad.nbits());
        assertEquals(AM_NEQ, 0, regLoad.dst());
        assertEquals(AM_NEQ, 0, regLoad.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ofsTooSmall() {
        regLoad.setOfsNbits(-1, 1);
    }

    @Test
    public void ofsMin() {
        print(EOL + "ofsMin()");
        regLoad.setOfsNbits(0, 1);
        print(regLoad);
        assertEquals(AM_NEQ, 0, regLoad.ofs());
    }

    @Test
    public void ofsMax() {
        print(EOL + "ofsMax()");
        regLoad.setOfsNbits(1023, 1);
        print(regLoad);
        assertEquals(AM_NEQ, 1023, regLoad.ofs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ofsTooBig() {
        regLoad.setOfsNbits(1024, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nbitsTooSmall() {
        regLoad.setOfsNbits(1, 0);
    }

    @Test
    public void nbitsMin() {
        print(EOL + "nbitsMin()");
        regLoad.setOfsNbits(1, 1);
        print(regLoad);
        assertEquals(AM_NEQ, 1, regLoad.nbits());
    }

    @Test
    public void nbitsMax() {
        print(EOL + "nbitsMax()");
        regLoad.setOfsNbits(1, 64);
        print(regLoad);
        assertEquals(AM_NEQ, 64, regLoad.nbits());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nbitsTooBig() {
        regLoad.setOfsNbits(1, 65);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dstTooSmall() {
        regLoad.setDst(-1);
    }

    @Test
    public void dstMin() {
        print(EOL + "dstMin()") ;
        regLoad.setDst(0);
        print(regLoad);
        assertEquals(AM_NEQ, 0, regLoad.dst());
    }


    @Test
    public void dstMax() {
        print(EOL + "dstMax()") ;
        regLoad.setDst(MAX_U32);
        print(regLoad);
        assertEquals(AM_NEQ, MAX_U32, regLoad.dst());
    }

    @Test(expected = IllegalArgumentException.class)
    public void dstTooBig() {
        regLoad.setDst(MAX_U32 + 1);
    }

    // NOTE: all long values are 64 bits, so there really are no useful tests
    // to make
    @Test
    public void nominalValue() {
        regLoad.setValue(0xffeedd);
        print(regLoad);
        assertEquals(AM_NEQ, 0xffeedd, regLoad.value());
    }

}
