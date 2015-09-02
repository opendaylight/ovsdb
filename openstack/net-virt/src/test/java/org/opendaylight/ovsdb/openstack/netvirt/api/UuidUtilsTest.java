/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.junit.Assert;
import org.junit.Test;

public class UuidUtilsTest {

    static final String NEUTRON_ID = "f1bf546b-79e8-4c42-93c1-147009c85c1d";
    static final String NEUTRON_KEY = "f1bf546b79e8c4293c1147009c85c1d";
    static final String INVALID_NEUTRON_ID = "f1bf546b-79e8-4c42-93c1-147009c85c1d-invalid";
    static final String KEYSTONE_ID = "879704f8cd7d4e60a41177fc99254b5e";
    static final String KEYSTONE_KEY = "879704f8cd7de60a41177fc99254b5e";
    static final String GARBAGE = "foobar12345";

    @Test
    public void testConvertNeutronIDToKey() throws Exception {
        Assert.assertNull(UuidUtils.convertNeutronIDToKey(null));
        Assert.assertNull(UuidUtils.convertNeutronIDToKey(INVALID_NEUTRON_ID));
        Assert.assertEquals(KEYSTONE_KEY,UuidUtils.convertNeutronIDToKey(KEYSTONE_ID));
        Assert.assertEquals(NEUTRON_KEY, UuidUtils.convertNeutronIDToKey(NEUTRON_ID));
        /* ToDo: Validation check in this method should be stricter
        This assertion should ideally fail, but it passes today
        */
        Assert.assertEquals(UuidUtils.convertNeutronIDToKey(GARBAGE), GARBAGE);
    }

    @Test
    public void testIsValidNeutronID() throws Exception {
        Assert.assertTrue(UuidUtils.isValidNeutronID(NEUTRON_ID));
        Assert.assertTrue(UuidUtils.isValidNeutronID(KEYSTONE_ID));
        Assert.assertFalse(UuidUtils.isValidNeutronID(INVALID_NEUTRON_ID));
        /* ToDo: Validation check in this method should be stricter
        This assertion should ideally fail, but it passes today
        */
        Assert.assertTrue(UuidUtils.isValidNeutronID(GARBAGE));
    }
}