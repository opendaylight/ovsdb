/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.lib.notation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionTest {

    @Test
    public void testToString() throws Exception {
        Version a = Version.fromString("1.2.3");
        assertEquals("1.2.3", a.toString());
    }

    @Test
    public void testEquals() throws Exception {
        Version a = Version.fromString("1.2.3");
        assertEquals(a, Version.fromString("1.2.3"));
        assertTrue(a.hashCode() == Version.fromString("1.2.3").hashCode());
        assertEquals(Version.NULL, Version.NULL);
        assertTrue(Version.NULL.hashCode() == Version.NULL.hashCode());
    }

    @Test
    public void testCompareTo() throws Exception {
        Version a = Version.fromString("1.2.27");
        Version b = Version.fromString("1.2.0");
        Version c = Version.fromString("1.99.0");
        Version d = Version.fromString("99.1.0");

        assertTrue(a.compareTo(b) > 0);
        assertTrue(b.compareTo(a) < 0);
        assertTrue(a.compareTo(c) < 0);
        assertTrue(c.compareTo(b) > 0);
        assertTrue(d.compareTo(a) > 0);
        assertTrue(b.compareTo(d) < 0);

    }
}
