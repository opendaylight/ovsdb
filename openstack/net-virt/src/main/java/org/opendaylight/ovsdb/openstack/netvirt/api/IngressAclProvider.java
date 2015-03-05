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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 *  This interface allows ingress Port Security flows to be written to devices
 */
public interface IngressAclProvider {

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
