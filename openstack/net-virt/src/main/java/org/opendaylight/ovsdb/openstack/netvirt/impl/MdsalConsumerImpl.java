/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.util.Set;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumerListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * MdsalConsumerImpl is the implementation for {@link MdsalConsumer}
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class MdsalConsumerImpl implements BindingAwareConsumer, MdsalConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalConsumerImpl.class);
    private static DataBroker dataBroker = null;

    private static Set<MdsalConsumerListener> mdsalConsumerListeners = Sets.newCopyOnWriteArraySet();
    private OvsdbDataChangeListener ovsdbDataChangeListener = null;
    private static MdsalUtils mdsalUtils = null;
    private volatile BindingAwareBroker broker; // dependency injection
    private ConsumerContext consumerContext = null;

    void init(Component c) {
        LOG.info(">>>>> Netvirt Provider Registered with MD-SAL");
        broker.registerConsumer(this, c.getDependencyManager().getBundleContext());
    }

    void destroy() {
        // Now lets close MDSAL session
        if (this.consumerContext != null) {
            //this.consumerContext.close();
            this.dataBroker = null;
            this.consumerContext = null;
        }
    }
    @Override
    public void onSessionInitialized (ConsumerContext consumerContext) {
        this.consumerContext = consumerContext;
        dataBroker = consumerContext.getSALService(DataBroker.class);
        LOG.info("netvirt MdsalConsumer initialized");
        ovsdbDataChangeListener = new OvsdbDataChangeListener(dataBroker);
        mdsalUtils = new MdsalUtils(dataBroker);
    }

    //@Override
    public static DataBroker getDataBroker () {
        return dataBroker;
    }

    private void listenerAdded(MdsalConsumerListener listener) {
        this.mdsalConsumerListeners.add(listener);
        LOG.info("listenerAdded: {}", listener);
    }

    private void listenerRemoved(MdsalConsumerListener listener) {
        this.mdsalConsumerListeners.remove(listener);
        LOG.info("listenerRemoved: {}", listener);
    }

    public InetAddress getTunnelEndPoint(Node node) {
        return null;
    }

    public String getNodeUUID(Node node) {
        return null;
    }

    @Override
    public String getBridgeUUID(String bridgeName) {
        return null;
    }

    // get vlan and network id

    public static Set<MdsalConsumerListener> getMdsalConsumerListeners () {
        return mdsalConsumerListeners;
    }

}
