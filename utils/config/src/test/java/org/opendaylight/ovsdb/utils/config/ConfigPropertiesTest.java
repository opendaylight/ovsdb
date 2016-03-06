/*
* Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.powermock.api.mockito.PowerMockito;
import org.osgi.framework.FrameworkUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {System.class, FrameworkUtil.class} )
 public class ConfigPropertiesTest {

    private static final String TEST_PROPERTY_KEY1 = "foobar34465$3467";
    private static final String TEST_PROPERTY_MOCK_VALUE1 = "xbarMock1";
    private static final String TEST_PROPERTY_KEY2 = "foobar34465$3468";
    private static final String TEST_PROPERTY_MOCK_VALUE2 = "xbarMock2";
    private static final String TEST_PROPERTY_KEY_NOT_FOUND = "foobarKey2_12445^%346";
    private static final String DEFAULT_PROPERTY_VALUE = "xbarDefaultValue";

    @Test
    public void testGetProperty() {
        Bundle bundleWithoutBundleNoContext = mock(Bundle.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext bundleContext = mock(BundleContext.class);

        // mock #1
        PowerMockito.mockStatic(FrameworkUtil.class);
        PowerMockito.when(FrameworkUtil.getBundle(this.getClass())).thenReturn(bundle);
        PowerMockito.when(FrameworkUtil.getBundle(ConfigPropertiesTestMockingBundleNoContext.class))
                .thenReturn(bundleWithoutBundleNoContext);
        // mock #2
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(bundleWithoutBundleNoContext.getBundleContext()).thenReturn(null);
        // mock #3
        when(bundleContext.getProperty(eq(TEST_PROPERTY_KEY1))).thenReturn(TEST_PROPERTY_MOCK_VALUE1);
        // mock #4
        // PowerMockito.mockStatic(System.class);
        // PowerMockito.when(System.getProperty(eq(TEST_PROPERTY_KEY2))).thenReturn(TEST_PROPERTY_MOCK_VALUE2);
        // NOTE: The mock #4 above is not supported by PowerMockito. To work around this limitation,
        // we will simply add the property explicitly into System (instead of the solution mentioned in link below).
        // See: http://javax0.wordpress.com/2013/01/29/how-to-mock-the-system-class/
        System.setProperty(TEST_PROPERTY_KEY2, TEST_PROPERTY_MOCK_VALUE2);

        // test 1. bundle is null, returned from a mock
        assertNull(FrameworkUtil.getBundle(ConfigPropertiesTestMocking.class));
        assertEquals(FrameworkUtil.getBundle(ConfigPropertiesTest.class), bundle);
        assertEquals(FrameworkUtil.getBundle(ConfigPropertiesTestMockingBundleNoContext.class),
                     bundleWithoutBundleNoContext);

        // test 2. bundleContext is null
        assertNull(bundleWithoutBundleNoContext.getBundleContext());
        assertEquals(bundle.getBundleContext(), bundleContext);

        // test 3. value returned from bundleContext.getProperty() is null.
        // Then System.getProperty() is called and can return a valid value if key is found.
        final String value31 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY2);
        assertEquals(TEST_PROPERTY_MOCK_VALUE2, value31);

        // test 4. value returned from ConfigProperties.getProperty is null
        final String value41 = ConfigProperties.getProperty(ConfigPropertiesTestMocking.class, TEST_PROPERTY_KEY1);
        assertNull(value41);  // class has no bundle
        final String value42 = ConfigProperties.getProperty(ConfigPropertiesTestMockingBundleNoContext.class,
                                                            TEST_PROPERTY_KEY1);
        assertNull(value42);  // class has no bundleContext
        final String value43 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY_NOT_FOUND);
        assertNull(value43);  // bundleContext will not know about key provided

        // test 5. value returned from ConfigProperties.getProperty is the default value provided
        final String value5 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY_NOT_FOUND, DEFAULT_PROPERTY_VALUE);
        assertEquals(DEFAULT_PROPERTY_VALUE, value5);

        // test 6. value returned from ConfigProperties.getProperty is the mocked value
        final String value61 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY1);
        assertEquals(TEST_PROPERTY_MOCK_VALUE1, value61);
        final String value62 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY1, DEFAULT_PROPERTY_VALUE);
        assertEquals(TEST_PROPERTY_MOCK_VALUE1, value62);
    }

    // Helper classes used to de-mux mock behaviors
    private class ConfigPropertiesTestMockingBundleNoContext {
    }
    private class ConfigPropertiesTestMocking {
    }
}
