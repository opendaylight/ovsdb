/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import org.opendaylight.ovsdb.openstack.netvirt.api.L2RewriteProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class L2RewriteService extends AbstractServiceInstance implements ConfigInterface, L2RewriteProvider {
    public L2RewriteService() {
        super(Service.L2_REWRITE);
    }

    public L2RewriteService(Service service) {
        super(service);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(L2RewriteProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}