/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.loadbalancer.internal;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class MDSALConsumer implements BindingAwareConsumer, IMDSALConsumer {

    private BundleContext ctx = null;
    private BindingAwareBroker broker = null;
    private ConsumerContext consumerContext = null;
    private DataBrokerService dataBrokerService;
    private PacketProcessingService packetProcessingService;
    private NotificationService notificationService;

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
        logger.info("OVSDB Loadbalancer Registered with MD-SAL");
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
        packetProcessingService = session.getRpcService(PacketProcessingService.class);
        notificationService = session.getSALService(NotificationService.class);
        logger.info("OVSDB Loadbalncer Session Initilized with CONSUMER CONTEXT {}", session.toString());
    }

    @Override
    public ConsumerContext getConsumerContext() {
        return consumerContext;
    }
    @Override
    public PacketProcessingService getPacketProcessingService() {
        return packetProcessingService;
    }
    @Override
    public NotificationService getNotificationService() {
        return notificationService;
    }
    @Override
    public DataBrokerService getDataBrokerService() {
        return dataBrokerService;
    }
}
