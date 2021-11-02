/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ConfigPropertiesTest {

    private static final String TEST_PROPERTY_KEY1 = "foobar34465$3467";
    private static final String TEST_PROPERTY_MOCK_VALUE1 = "xbarMock1";
    private static final String TEST_PROPERTY_KEY2 = "foobar34465$3468";
    private static final String TEST_PROPERTY_MOCK_VALUE2 = "xbarMock2";
    private static final String TEST_PROPERTY_KEY_NOT_FOUND = "foobarKey2_12445^%346";
    private static final String DEFAULT_PROPERTY_VALUE = "xbarDefaultValue";

    @Test
    public void testGetProperty() {
        try (var frameworkUtil = mockStatic(FrameworkUtil.class)) {
            final var bundleWithoutBundleNoContext = mock(Bundle.class);
            final var bundle = mock(Bundle.class);

            // mock #1
            frameworkUtil.when(() -> FrameworkUtil.getBundle(getClass()))
                .thenReturn(bundle);
            frameworkUtil.when(() -> FrameworkUtil.getBundle(ConfigPropertiesTestMockingBundleNoContext.class))
                .thenReturn(bundleWithoutBundleNoContext);

            // mock #2
            BundleContext bundleContext = mock(BundleContext.class);
            when(bundle.getBundleContext()).thenReturn(bundleContext);
            when(bundleWithoutBundleNoContext.getBundleContext()).thenReturn(null);
            // mock #3
            when(bundleContext.getProperty(TEST_PROPERTY_KEY1)).thenReturn(TEST_PROPERTY_MOCK_VALUE1);
            when(bundleContext.getProperty(TEST_PROPERTY_KEY2)).thenReturn(null);
            when(bundleContext.getProperty(TEST_PROPERTY_KEY_NOT_FOUND)).thenReturn(null);

            // mock #4, Mockito says:
            //    It is not possible to mock static methods of java.lang.System to avoid interfering with class loading
            //    what leads to infinite loops
            //
            // To work around this limitation, we will simply add the property explicitly into System
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
            assertEquals(TEST_PROPERTY_MOCK_VALUE2, ConfigProperties.getProperty(getClass(), TEST_PROPERTY_KEY2));

            // test 4. value returned from ConfigProperties.getProperty is null
            final String value41 = ConfigProperties.getProperty(ConfigPropertiesTestMocking.class, TEST_PROPERTY_KEY1);
            assertNull(value41);  // class has no bundle
            final String value42 = ConfigProperties.getProperty(ConfigPropertiesTestMockingBundleNoContext.class,
                TEST_PROPERTY_KEY1);
            assertNull(value42);  // class has no bundleContext
            final String value43 = ConfigProperties.getProperty(getClass(), TEST_PROPERTY_KEY_NOT_FOUND);
            assertNull(value43);  // bundleContext will not know about key provided

            // test 5. value returned from ConfigProperties.getProperty is the default value provided
            assertEquals(DEFAULT_PROPERTY_VALUE,
                ConfigProperties.getProperty(getClass(), TEST_PROPERTY_KEY_NOT_FOUND, DEFAULT_PROPERTY_VALUE));

            // test 6. value returned from ConfigProperties.getProperty is the mocked value
            assertEquals(TEST_PROPERTY_MOCK_VALUE1, ConfigProperties.getProperty(getClass(), TEST_PROPERTY_KEY1));
            assertEquals(TEST_PROPERTY_MOCK_VALUE1,
                ConfigProperties.getProperty(getClass(), TEST_PROPERTY_KEY1, DEFAULT_PROPERTY_VALUE));
        }
    }

    // Helper classes used to de-mux mock behaviors
    private static class ConfigPropertiesTestMockingBundleNoContext {
    }

    private static class ConfigPropertiesTestMocking {
    }
}
