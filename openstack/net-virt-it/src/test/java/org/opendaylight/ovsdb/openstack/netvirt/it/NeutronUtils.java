package org.opendaylight.ovsdb.openstack.netvirt.it;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;

public class NeutronUtils {
    public NeutronPort createNeutronPort(String networkId, String subnetId,
                                         String id, String owner, String ipaddr, String mac) {
        INeutronPortCRUD iNeutronPortCRUD =
                (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        NeutronPort np = new NeutronPort();
        np.initDefaults();
        np.setID(id);
        np.setDeviceOwner(owner);
        np.setMacAddress(mac);
        np.setNetworkUUID(networkId);
        List<org.opendaylight.neutron.spi.Neutron_IPs> srcAddressList =
                new ArrayList<>();
        org.opendaylight.neutron.spi.Neutron_IPs nip = new org.opendaylight.neutron.spi.Neutron_IPs();
        nip.setIpAddress(ipaddr);
        nip.setSubnetUUID(subnetId);
        srcAddressList.add(nip);
        np.setFixedIPs(srcAddressList);
        List<NeutronSecurityGroup> nsgs = new ArrayList<>();
        np.setSecurityGroups(nsgs);
        iNeutronPortCRUD.add(np);
        return np;
    }

    public NeutronSubnet createNeutronSubnet(String subnetId, String tenantId,
                                              String networkId, String cidr) {
        INeutronSubnetCRUD iNeutronSubnetCRUD =
                (INeutronSubnetCRUD) ServiceHelper.getGlobalInstance(INeutronSubnetCRUD.class, this);
        NeutronSubnet ns = new NeutronSubnet();
        ns.setID(subnetId);
        ns.setCidr(cidr);
        ns.initDefaults();
        ns.setNetworkUUID(networkId);
        ns.setTenantID(tenantId);
        iNeutronSubnetCRUD.add(ns);
        return ns;
    }

    public NeutronNetwork createNeutronNetwork(String uuid, String tenantID, String networkTypeVxlan, String segId) {
        INeutronNetworkCRUD iNeutronNetworkCRUD =
                (INeutronNetworkCRUD) ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        NeutronNetwork nn = new NeutronNetwork();
        nn.setID(uuid);
        nn.initDefaults();
        nn.setTenantID(tenantID);
        nn.setProviderNetworkType(networkTypeVxlan);
        nn.setProviderSegmentationID(segId);
        iNeutronNetworkCRUD.addNetwork(nn);
        return nn;
    }
}
