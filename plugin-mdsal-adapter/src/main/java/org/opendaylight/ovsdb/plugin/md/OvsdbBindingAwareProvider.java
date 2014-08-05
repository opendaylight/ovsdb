package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;

/**
 * Created by dave on 01/08/2014.
 */
public interface OvsdbBindingAwareProvider {
    public DataBroker getDataBroker();
    public NotificationService getNotificationService();
}
