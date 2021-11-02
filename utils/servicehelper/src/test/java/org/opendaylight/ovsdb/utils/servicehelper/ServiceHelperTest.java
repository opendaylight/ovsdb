/*
 * Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.servicehelper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.FrameworkUtil;
import org.springframework.osgi.mock.MockBundle;

/**
 * JUnit test for {@link ServiceHelper}.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ServiceHelperTest {
    /**
     * Test method for {@link ServiceHelper#getGlobalInstance(Class, Object)}.
     */
    @Test
    public void getGlobalInstanceTest() {
        try (var frameworkUtil = mockStatic(FrameworkUtil.class)) {
            final var mockBundle = new MockBundle();
            frameworkUtil.when(() -> FrameworkUtil.getBundle(ServiceHelperTest.class)).thenReturn(null, mockBundle);

            assertNull(ServiceHelper.getGlobalInstance(Test.class, this));
            assertNotNull(ServiceHelper.getGlobalInstance(Test.class, this));
        }
    }
}
