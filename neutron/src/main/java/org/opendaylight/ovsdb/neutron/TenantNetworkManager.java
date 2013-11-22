package org.opendaylight.ovsdb.neutron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.opendaylight.controller.containermanager.ContainerConfig;
import org.opendaylight.controller.containermanager.ContainerFlowConfig;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManager.class);

    private static final int MAX_VLAN = 4096;
    private static TenantNetworkManager tenantHelper = new TenantNetworkManager();
    private Queue<Integer> internalVlans = new LinkedList<Integer>();
    private Map<String, Integer> tenantVlanMap = new HashMap<String, Integer>();
    private TenantNetworkManager() {
        for (int i = 1; i < MAX_VLAN ; i++) {
            internalVlans.add(i);
        }
    }

    public static TenantNetworkManager getManager() {
        return tenantHelper;
    }

    private int assignInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) return mappedVlan;
        mappedVlan = internalVlans.poll();
        if (mappedVlan != null) tenantVlanMap.put(networkId, mappedVlan);
        return mappedVlan;
    }

    public void internalVlanInUse (int vlan) {
        internalVlans.remove(vlan);
    }

    public int getInternalVlan (String networkId) {
        return tenantVlanMap.get(networkId);
    }

    public int networkCreated (String networkId) {
        int internalVlan = this.assignInternalVlan(networkId);
        if (internalVlan != 0) {
            ContainerConfig config = new ContainerConfig();
            config.setContainer(networkId);
            ContainerFlowConfig flowConfig = new ContainerFlowConfig("InternalVlan", internalVlan+"",
                    null, null, null, null, null);
            List<ContainerFlowConfig> containerFlowConfigs = new ArrayList<ContainerFlowConfig>();
            containerFlowConfigs.add(flowConfig);
            config.addContainerFlows(containerFlowConfigs);
            IContainerManager containerManager = (IContainerManager)ServiceHelper.getGlobalInstance(IContainerManager.class, this);
            if (containerManager != null) {
                Status status = containerManager.addContainer(config);
                logger.debug("Network Creation Status : {}", status.toString());
            } else {
                logger.error("ContainerManager is null. Failed to create Container for {}", networkId);
            }
        }
        return internalVlan;
    }
}
