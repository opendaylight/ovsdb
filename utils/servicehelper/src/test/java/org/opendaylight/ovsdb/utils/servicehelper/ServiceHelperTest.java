/*
 *  Copyright (C) 2015 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.utils.servicehelper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.osgi.mock.MockBundle;

/**
 * JUnit test for
 * {@link ServiceHelper}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(FrameworkUtil.class)
public class ServiceHelperTest {
    @Test
    /**
     * Test method for
     * {@link ServiceHelper#getGlobalInstance(Class, Object)}
     */
    public void getGlobalInstanceTest () {
        Bundle bundle = new MockBundle();

        PowerMockito.mockStatic(FrameworkUtil.class);

        PowerMockito.when(FrameworkUtil.getBundle(any(Class.class)))
                .thenReturn(null);
        Object object = ServiceHelper.getGlobalInstance(Test.class, this);
        assertNull("Service should be null", object);

        PowerMockito.when(FrameworkUtil.getBundle(any(Class.class)))
                .thenReturn(bundle);
        object = ServiceHelper.getGlobalInstance(Test.class, this);
        assertNotNull("Service should not be null", object);
    }
}
