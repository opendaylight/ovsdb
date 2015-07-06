/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.neutron.spi.NeutronSecurityGroup;

/**
 *  This interface allows ingress Port Security flows to be written to devices
 */
public interface IngressAclProvider {

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
     * Program fixed ingress ACL rules that will be associated with the VM port when a vm is spawned.
     * *
     * @param dpid the dpid
     * @param segmentationId the segmentation id
     * @param attachedMac the attached mac
     * @param localPort the local port
     * @param isLastPortinSubnet is this the last port in the subnet
     * @param write is this flow writing or deleting
     */
    public void programFixedSecurityACL(Long dpid, String segmentationId,
            String attachedMac, long localPort, boolean isLastPortinSubnet, boolean write);
}
