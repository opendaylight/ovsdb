/*
 * Copyright (c) 2014, 2015 HP, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

/**
 *  This interface maintain a mapping between the security group and the ports
 *  have this security group as a remote security group. Whenever a new port is
 *  added with a security group associated with it, a rule will be added to allow
 *  traffic from/to the vm from  the vms which has the former as a remote sg in its rule.
 *
 *  @author Aswin Suryanarayanan.
 */

public interface SecurityGroupCacheManger {

    /**
     * Notifies that a new port in the security group with securityGroupUuid.
     * @param securityGroupUuid the uuid of the security group associated with the port.
     * @param portUuid the uuid of the port.
     */
    void portAdded(String securityGroupUuid, String portUuid);
    /**
     * Notifies that a port is removed with the securityGroupUuid.
     * @param securityGroupUuid the uuid of the security group associated with the port.
     * @param portUuid the uuid of the port.
     */
    void portRemoved(String securityGroupUuid, String portUuid);
    /**
     * A port with portUuid has a reference remote security group remoteSgUuid will be added
     * to the cache maintained.
     * @param remoteSgUuid the remote security group uuid.
     * @param portUuid the uuid of the port.
     */
    void addToCache(String remoteSgUuid, String portUuid);
    /**A port with portUUID has a reference remote security group remoteSgUuid will be removed
     * from the cache maintained.
     * @param remoteSgUuid the remote security group uuid.
     * @param portUuid portUUID the uuid of the port.
     */
    void removeFromCache(String remoteSgUuid, String portUuid);
}