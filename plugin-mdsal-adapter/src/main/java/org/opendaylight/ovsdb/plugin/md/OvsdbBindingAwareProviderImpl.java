/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;

import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbBindingAwareProviderImpl extends AbstractBindingAwareProvider implements OvsdbBindingAwareProvider {

    private DataBroker dataBroker;
    private NotificationProviderService notificationService;

    static final Logger logger = LoggerFactory.getLogger(OvsdbBindingAwareProvider.class);

    private BundleContext bc;
    private volatile BindingAwareBroker broker;

    void init(Component c) {
        this.bc = c.getDependencyManager().getBundleContext();
        broker.registerProvider(this, this.bc);
        logger.info("OVSDB MD-SAL Inventory Adapter Registered With the MD-SAL");
    }

    void destroy() {
        this.dataBroker = null;
        this.notificationService = null;
    }

    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext providerContext) {
        this.dataBroker = providerContext.getSALService(DataBroker.class);
        this.notificationService = providerContext.getSALService(NotificationProviderService.class);
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public NotificationService getNotificationService() {
        return this.notificationService;
    }
}
