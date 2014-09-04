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
    public void handleLocalInPort(Long dpidLong, Short writeTable, Short goToTableId,
            String segmentationId, Long inPort, String attachedMac,
            boolean write);
}
