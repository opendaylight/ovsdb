package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routemgr.impl.rev141210;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.ovsdb.routemgr.net.PktHandler;
import org.opendaylight.ovsdb.routemgr.net.NetDataListener;

public class RoutemgrImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routemgr.impl.rev141210.AbstractRoutemgrImplModule {

    private final static Logger LOG = LoggerFactory.getLogger(RoutemgrImplModule.class);
    private NetDataListener     netDataListener;
    private PktHandler          ipPktHandler;
    private Registration        packetListener = null;

    public RoutemgrImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RoutemgrImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routemgr.impl.rev141210.RoutemgrImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        LOG.info("createInstance invoked for the routemgr module.");
        NotificationProviderService notificationService = getNotificationServiceDependency();
        DataBroker dataService = getDataBrokerDependency();
        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();
        SalFlowService salFlowService = rpcRegistryDependency.getRpcService(SalFlowService.class);

        netDataListener = new NetDataListener (dataService);
        netDataListener.registerDataChangeListener();

        ipPktHandler = new PktHandler();
        ipPktHandler.setDataBrokerService(dataService);
        packetListener = notificationService.registerNotificationListener(ipPktHandler);
        LOG.debug ("started the packethandler to receive lacp pdus");

        PacketProcessingService packetProcessingService =
                rpcRegistryDependency.getRpcService(PacketProcessingService.class);

	/* TODO:: Spawn the Default threads - PDU Decoder and Tx Threads */



        LOG.debug("starting to read from data store");
        netDataListener.readDataStore();

        final class CloseLacpResources implements AutoCloseable {
            @Override
            public void close() throws Exception {
                if (packetListener != null)
                {
                    packetListener.close();
                }

                if(netDataListener != null) {
                    netDataListener.closeListeners();
                }
                LOG.info("closed the listeners for routemgr. Clearing the cached info.");

                return;
            }
        }

        AutoCloseable ret = new CloseLacpResources();
        LOG.info("Routemgr (instance {}) initialized.", ret);
        return ret;
    }

}
