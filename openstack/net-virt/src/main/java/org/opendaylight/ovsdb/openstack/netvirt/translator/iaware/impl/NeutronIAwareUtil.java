/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronIAwareUtil {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(NeutronIAwareUtil.class);

    private NeutronIAwareUtil() {
    }

    public static Object[] getInstances(Class<?> clazz,Object bundle) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference<?>[] services = null;
                services = bCtx.getServiceReferences(clazz.getName(),
                        null);
            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Instance reference is NULL", e);
        }
        return instances;
    }

}
