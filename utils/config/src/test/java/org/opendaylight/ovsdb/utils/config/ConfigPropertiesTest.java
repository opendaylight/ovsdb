/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Flavio Fernandes
*/
package org.opendaylight.ovsdb.utils.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ConfigPropertiesTest {

    private static final String TEST_PROPERTY_KEY = "foobar34465$3467";
    private static final String DEFAULT_PROPERTY_VALUE = "xbar";

    @Test
    public void testGetProperty() {
        final String value1 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY);

        final String value2 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY,
                                                               DEFAULT_PROPERTY_VALUE);

        assertNull(value1);
        assertEquals(value2, DEFAULT_PROPERTY_VALUE);
    }
}
