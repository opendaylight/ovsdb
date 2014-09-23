/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

/**
 *  This interface allows L2Forwarding flows to be written to devices
 */
public interface L2ForwardingProvider {
    public void programLocalUcastOut(Long dpidLong, String segmentationId, Long localPort, String attachedMac, boolean write);
    public void programLocalVlanUcastOut(Long dpidLong, String segmentationId, Long localPort, String attachedMac, boolean write);
    public void programLocalBcastOut(Long dpidLong, String segmentationId, Long localPort, boolean write);
    public void programLocalVlanBcastOut(Long dpidLong, String segmentationId, Long localPort, Long ethPort, boolean write);
    public void programLocalTableMiss(Long dpidLong, String segmentationId, boolean write);
    public void programLocalVlanTableMiss(Long dpidLong, String segmentationId, boolean write);
    public void programTunnelOut(Long dpidLong, String segmentationId, Long OFPortOut, String attachedMac, boolean write);
    public void programVlanOut(Long dpidLong, String segmentationId, Long ethPort, String attachedMac, boolean write);
    public void programTunnelFloodOut(Long dpidLong, String segmentationId, Long OFPortOut, boolean write);
    public void programVlanFloodOut(Long dpidLong, String segmentationId, Long ethPort, boolean write);
    public void programTunnelMiss(Long dpidLong, String segmentationId, boolean write);
    public void programVlanMiss(Long dpidLong, String segmentationId, Long ethPort, boolean write);
}
