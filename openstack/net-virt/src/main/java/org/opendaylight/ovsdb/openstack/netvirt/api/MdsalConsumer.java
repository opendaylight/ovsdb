/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.sal.binding.api.NotificationProviderService;

/**
 * MdsalConsumer is the interface to the mdsal for netvirt.
 *
 * @author Sam Hague (shague@redhat.com)
 */
public interface MdsalConsumer {
    //public ConsumerContext getConsumerContext();
    public static DataBroker dataBroker = null;
    //public NotificationProviderService getNotificationService();
}
