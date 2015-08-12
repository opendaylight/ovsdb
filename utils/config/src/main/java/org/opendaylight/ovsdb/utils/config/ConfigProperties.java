/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.config;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public final class ConfigProperties {

    private ConfigProperties() {
        // empty
    }

    public static String getProperty(Class<?> classParam, final String propertyStr) {
        return getProperty(classParam, propertyStr, null);
    }

    public static String getProperty(Class<?> classParam, final String propertyStr, final String defaultValue) {
        String value = null;
        Bundle bundle = FrameworkUtil.getBundle(classParam);

        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext != null) {
                value = bundleContext.getProperty(propertyStr);
            }
        }
        if (value == null) {
            value = System.getProperty(propertyStr, defaultValue);
        }
        return value;
    }
}
