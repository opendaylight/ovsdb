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
 *  This interface allows Classifier flows to be written to devices
 */
public interface ClassifierProvider {
    public void programLocalInPort(Long dpidLong, String segmentationId, Long inPort, String attachedMac, boolean write);
    public void programLocalInPortSetVlan(Long dpidLong, String segmentationId, Long inPort, String attachedMac, boolean write);
    public void programDropSrcIface(Long dpidLong, Long inPort, boolean write);
    public void programTunnelIn(Long dpidLong, String segmentationId, Long ofPort, boolean write);
    public void programVlanIn(Long dpidLong, String segmentationId, Long ethPort, boolean write);
    public void programLLDPPuntRule(Long dpidLong);
}
