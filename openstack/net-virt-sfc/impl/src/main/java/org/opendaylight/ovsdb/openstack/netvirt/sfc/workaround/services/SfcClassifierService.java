/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class SfcClassifierService extends AbstractServiceInstance implements ConfigInterface {
    public SfcClassifierService(Service service) {
        super(service);
    }

    public SfcClassifierService() {
        super(Service.SFC_CLASSIFIER);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(SfcClassifierService.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
