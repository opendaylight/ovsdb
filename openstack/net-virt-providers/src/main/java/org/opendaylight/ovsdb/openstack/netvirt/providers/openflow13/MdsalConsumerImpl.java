/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import java.util.Collection;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class MdsalConsumerImpl implements BindingAwareConsumer, MdsalConsumer, BindingAwareProvider {

    private BundleContext ctx = null;
    private volatile BindingAwareBroker broker;
    private ConsumerContext consumerContext = null;
    private DataBroker dataBroker;
    private NotificationProviderService notificationService;

    static final Logger logger = LoggerFactory.getLogger(MdsalConsumerImpl.class);

    void init(Component c) {
        this.ctx = c.getDependencyManager().getBundleContext();
        logger.info("Open vSwitch OpenFlow 1.3 Neutron Networking Provider Registered with MD-SAL");
        broker.registerConsumer(this, this.ctx);
        broker.registerProvider(this, this.ctx);
    }

    void destroy() {
        // Now lets close MDSAL session
        if (this.consumerContext != null) {
            //this.consumerContext.close();
            this.dataBroker = null;
            this.consumerContext = null;
        }
    }

    void start() {
    }

    void stop() {
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        this.consumerContext = session;
        dataBroker = session.getSALService(DataBroker.class);
        logger.info("OVSDB Neutron Session Initialized with CONSUMER CONTEXT {}", session.toString());
    }

    @Override
    public ConsumerContext getConsumerContext() {
        return consumerContext;
    }
    @Override
    public DataBroker getDataBroker() {
        return dataBroker;
    }
    @Override
    public NotificationProviderService getNotificationService() {
        return notificationService;
    }

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        notificationService = session.getSALService(NotificationProviderService.class);
    }
}
