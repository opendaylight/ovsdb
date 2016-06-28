/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.notation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

    /**
     * Test to verify if Version object can be constructed from a string
     * and to verify if Version.toString() yields original string used
     * during construction of the object.
     */
    @Test
    public void testToString() throws Exception {
        Version version123 = Version.fromString("1.2.3");
        assertEquals("1.2.3", version123.toString());
    }

    /**
     * Test to verify if equals() and hashCode() methods work
     * for the Version object.
     */
    @Test
    public void testEquals() throws Exception {
        Version version123 = Version.fromString("1.2.3");
        Version version0 = Version.fromString("0.0.0");
        assertEquals(version123, Version.fromString("1.2.3"));
        assertTrue(version123.hashCode() == Version.fromString("1.2.3").hashCode());
        assertEquals(version0, Version.fromString("0.0.0"));
        assertTrue(version0.hashCode() == Version.fromString("0.0.0").hashCode());
    }

    /**
     * Test to verify compareTo() function works for the
     * X.Y.Z semantics of the version number.
     */
    @Test
    public void testCompareTo() throws Exception {
        Version version1227 = Version.fromString("1.2.27");
        Version version1200 = Version.fromString("1.2.0");

        assertTrue(version1227.compareTo(version1200) > 0);
        assertTrue(version1200.compareTo(version1227) < 0);

        Version version1990 = Version.fromString("1.99.0");
        assertTrue(version1227.compareTo(version1990) < 0);
        assertTrue(version1990.compareTo(version1200) > 0);

        Version version9910 = Version.fromString("99.1.0");
        assertTrue(version9910.compareTo(version1227) > 0);
        assertTrue(version1200.compareTo(version9910) < 0);

    }

    /* TODO: Incomplete compare test
     */
    @Test
    public void testCompare() throws Exception {
        Version.fromString("6.9.3");
        Version.fromString("7.1.0");
    }
}
