package org.opendaylight.ovsdb.loadbalancer.internal;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

public interface IMDSALConsumer {
    public ConsumerContext getConsumerContext();
    public DataBrokerService getDataBrokerService();
    public PacketProcessingService getPacketProcessingService();
    public NotificationService getNotificationService();

}
