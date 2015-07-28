package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;

import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.Neutron_IPs;

/**
 *  This interface allows egress Port Security flows to be written to devices
 */
public interface EgressAclProvider {

    /**
     * Program port security ACL.
     *
     * @param dpid the dpid
     * @param segmentationId the segmentation id
     * @param attachedMac the attached mac
     * @param localPort the local port
     * @param securityGroup the security group
     * @param srcAddressList the src address associated with the vm port
     * @param write  is this flow write or delete
     */
    void programPortSecurityACL(Long dpid, String segmentationId, String attachedMac,
                                       long localPort, NeutronSecurityGroup securityGroup,
                                       List <Neutron_IPs> srcAddressList, boolean write);
    /**
     *  Program fixed egress ACL rules that will be associated with the VM port when a vm is spawned.
     *
     * @param dpid the dpid
     * @param segmentationId the segmentation id
     * @param attachedMac the attached mac
     * @param localPort the local port
     * @param srcAddressList the list of source ip address assigned to vm
     * @param isLastPortinBridge is this the last port in the bridge
     * @param isComputePort indicates whether this port is a compute port or not
     * @param write is this flow writing or deleting
     */
    void programFixedSecurityACL(Long dpid, String segmentationId,String attachedMac,
                                        long localPort, List<Neutron_IPs> srcAddressList, boolean isLastPortinBridge, boolean isComputePort, boolean write);
}