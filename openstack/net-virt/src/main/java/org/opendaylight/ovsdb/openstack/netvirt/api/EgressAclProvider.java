package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.neutron.spi.NeutronSecurityGroup;

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
     */
    public void programPortSecurityACL(Long dpid, String segmentationId, String attachedMac,
                                       long localPort, NeutronSecurityGroup securityGroup);
    /**
     *  Program fixed egress ACL rules that will be associated with the VM port when a vm is spawned.
     *
     * @param dpid the dpid
     * @param segmentationId the segmentation id
     * @param attachedMac the attached mac
     * @param localPort the local port
     * @param isLastPortinBridge is this the last port in the bridge
     * @param write is this flow writing or deleting
     */
    public void programFixedSecurityACL(Long dpid, String segmentationId,String attachedMac,
            long localPort, boolean isLastPortinBridge, boolean write);
}
