/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

/**
 *  This interface allows L2Forwarding flows to be written to devices
 */
public interface L2ForwardingProvider {
    void programLocalUcastOut(Long dpidLong, String segmentationId, Long localPort, String attachedMac, boolean write);
    void programLocalVlanUcastOut(Long dpidLong, String segmentationId, Long localPort, String attachedMac, boolean write);
    void programLocalBcastOut(Long dpidLong, String segmentationId, Long localPort, boolean write);
    void programLocalVlanBcastOut(Long dpidLong, String segmentationId, Long localPort, Long ethPort, boolean write);
    void programLocalTableMiss(Long dpidLong, String segmentationId, boolean write);
    void programLocalVlanTableMiss(Long dpidLong, String segmentationId, boolean write);
    void programTunnelOut(Long dpidLong, String segmentationId, Long OFPortOut, String attachedMac, boolean write);
    void programVlanOut(Long dpidLong, String segmentationId, Long ethPort, String attachedMac, boolean write);
    void programTunnelFloodOut(Long dpidLong, String segmentationId, Long OFPortOut, boolean write);
    void programVlanFloodOut(Long dpidLong, String segmentationId, Long ethPort, boolean write);
    void programTunnelMiss(Long dpidLong, String segmentationId, boolean write);
    void programVlanMiss(Long dpidLong, String segmentationId, Long ethPort, boolean write);
}
