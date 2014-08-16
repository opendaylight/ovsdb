package org.opendaylight.ovsdb.loadbalancer;



import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;

import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoadBalancer implements IListenDataPacket {

    private static final Logger logger = LoggerFactory
            .getLogger(LoadBalancer.class);
    private ISwitchManager switchManager = null;
    private IFlowProgrammerService programmer = null;
    private IDataPacketService dataPacketService = null;


    void setDataPacketService(IDataPacketService s) {
            this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setFlowProgrammerService(IFlowProgrammerService s)
    {
        this.programmer = s;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService s) {
        if (this.programmer == s) {
            this.programmer = null;
        }
    }

    void setSwitchManager(ISwitchManager s) {
        logger.debug("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.info("Initialized");
        // Disabling the SimpleForwarding and ARPHandler bundle to not conflict with this one
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        for(Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("arphandler") ||
                    bundle.getSymbolicName().contains("simpleforwarding"))  {
                try {
                    bundle.uninstall();
                } catch (BundleException e) {
                    logger.error("Exception in Bundle uninstall "+bundle.getSymbolicName(), e);
                }
            }
        }

    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
    * Function called by dependency manager after "init ()" is called
    * and after the services provided by the class are registered in
    * the service registry
    *
    */
    void start() {
        logger.info("Started");
    }
    
    /**
    * Function called by the dependency manager before the services
    * exported by the component are unregistered, this will be
    * followed by a "destroy ()" calls
    *
    */
    void stop() {
        logger.info("Stopped");
    }
    



@Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
}


        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);

        if (formattedPak instanceof Ethernet) {
            return PacketResult.IGNORED;
        } else {
            return PacketResult.CONSUME;
        }
}
}

