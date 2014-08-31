/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Madhu Venugopal
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

/**
 * A PipelineOrchestrator provides the necessary orchestration logic to allow multiple network services
 * to share a common OpenFlow 1.3 based multi-table pipeline.
 */
public interface PipelineOrchestrator {
    void registerService(Service service, AbstractServiceInstance serviceInstance);
    void unregisterService(Service service);
    public Service getNextServiceInPipeline(Service service);
    AbstractServiceInstance getServiceInstance(Service service);
}
