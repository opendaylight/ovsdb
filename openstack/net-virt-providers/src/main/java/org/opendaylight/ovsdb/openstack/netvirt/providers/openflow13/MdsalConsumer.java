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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;

public interface MdsalConsumer {
    public ConsumerContext getConsumerContext();
    public DataBroker getDataBroker();
    public NotificationProviderService getNotificationService();
    public void notifyFlowCapableNodeCreateEvent(String openFlowId, Action action);
}
