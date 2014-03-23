/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.neutron.impl;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.ovsdb.neutron.IMDSALConsumer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class MDSALConsumer implements BindingAwareConsumer, IMDSALConsumer {

    private BundleContext ctx = null;
    private BindingAwareBroker broker = null;
    private ConsumerContext consumerContext = null;
    private DataBrokerService dataBrokerService;

    static final Logger logger = LoggerFactory.getLogger(MDSALConsumer.class);

    void setBindingAwareBroker (BindingAwareBroker b) {
        this.broker = b;
    }

    void unsetBindingAwareBroker(BindingAwareBroker b) {
        if (this.broker == b) {
            this.broker = null;
        }
    }

    void init(Component c) {
        this.ctx = c.getDependencyManager().getBundleContext();
        logger.info("OVSDB Neutron Registered with MD-SAL");
        broker.registerConsumer(this, this.ctx);
    }

    void destroy() {
        // Now lets close MDSAL session
        if (this.consumerContext != null) {
            //this.consumerContext.close();
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
        dataBrokerService = session.getSALService(DataBrokerService.class);
        logger.info("OVSDB Neutron Session Initilized with CONSUMER CONTEXT {}", session.toString());
    }

    @Override
    public ConsumerContext getConsumerContext() {
        return consumerContext;
    }
    @Override
    public DataBrokerService getDataBrokerService() {
        return dataBrokerService;
    }
}
