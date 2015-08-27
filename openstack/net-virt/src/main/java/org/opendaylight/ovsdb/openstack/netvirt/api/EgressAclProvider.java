/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;

/**
 *  This interface allows egress Port Security flows to be written to devices.
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
    void programPortSecurityAcl(Long dpid, String segmentationId, String attachedMac,
                                       long localPort, NeutronSecurityGroup securityGroup,
                                       List<Neutron_IPs> srcAddressList, boolean write);
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
    void programFixedSecurityAcl(Long dpid, String segmentationId,String attachedMac, long localPort,
                                  List<Neutron_IPs> srcAddressList, boolean isLastPortinBridge,
                                  boolean isComputePort, boolean write);
}