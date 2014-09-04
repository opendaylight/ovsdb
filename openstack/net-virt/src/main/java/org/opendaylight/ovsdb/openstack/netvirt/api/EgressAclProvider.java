package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.sal.core.Node;

/**
 *  This interface allows egress Port Security flows to be written to devices
 */
public interface EgressAclProvider {

    /**
     * Program port security ACL.
     *
     * @param node the node
     * @param dpid the dpid
     * @param segmentationId the segmentation id
     * @param attachedMac the attached mac
     * @param localPort the local port
     * @param securityGroup the security group
     */
    public void programPortSecurityACL(Node node, Long dpid, String segmentationId, String attachedMac,
                                       long localPort, NeutronSecurityGroup securityGroup);
}
