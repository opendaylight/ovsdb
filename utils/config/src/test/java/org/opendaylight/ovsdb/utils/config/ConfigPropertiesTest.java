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
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.System;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.powermock.api.mockito.PowerMockito;
import org.osgi.framework.FrameworkUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {FrameworkUtil.class} )
 public class ConfigPropertiesTest {

    private static final String TEST_PROPERTY_KEY1 = "foobar34465$3467";
    private static final String TEST_PROPERTY_MOCK_VALUE1 = "xbarMock1";
    private static final String TEST_PROPERTY_KEY2 = "foobarKey2_12445^%346";
    private static final String DEFAULT_PROPERTY_VALUE = "xbarDefaultValue";

    @Test
    public void testGetPropertyNoMock() {
        final String value1 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY1);
        assertNull(value1);

        final String value2 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY1,
                                                               DEFAULT_PROPERTY_VALUE);
        assertEquals(value2, DEFAULT_PROPERTY_VALUE);
    }

    @Test
    public void testGetProperty() {
        Bundle bundle = mock(Bundle.class);
        BundleContext bundleContext = mock(BundleContext.class);

        // mock #1
        PowerMockito.mockStatic(FrameworkUtil.class);
        PowerMockito.when(FrameworkUtil.getBundle(this.getClass())).thenReturn(bundle);

        // mock #2
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getProperty(eq(TEST_PROPERTY_KEY1))).thenReturn(TEST_PROPERTY_MOCK_VALUE1);

        // ask for key1 and expect value1 due to mock#1 and mock#2
        final String value1 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY1,
                                                           DEFAULT_PROPERTY_VALUE);
        assertEquals(TEST_PROPERTY_MOCK_VALUE1, value1);

        // ask for key1 and expect null due to unexpected bundle class (System)
        final String value2 = ConfigProperties.getProperty(System.class, TEST_PROPERTY_KEY1);
        assertNull(value2);

        // ask for key2 and expect defaultValue due to default parameter
        final String value3 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY2,
                                                           DEFAULT_PROPERTY_VALUE);
        assertEquals(DEFAULT_PROPERTY_VALUE, value3);
    }

}
