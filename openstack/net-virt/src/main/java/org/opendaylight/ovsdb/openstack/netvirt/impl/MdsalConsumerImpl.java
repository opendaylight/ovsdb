/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumerListener;
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
    private Set<MdsalConsumerListener> mdsalConsumerListeners = Sets.newCopyOnWriteArraySet();
    private OvsdbDataChangeListener ovsdbDataChangeListener = null;

    @Override
    public void onSessionInitialized (ConsumerContext consumerContext) {
        dataBroker = consumerContext.getSALService(DataBroker.class);
        LOG.info("netvirt MdsalConsumer initialized");
        ovsdbDataChangeListener = new OvsdbDataChangeListener(dataBroker);
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

}
