/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;

/**
 * A PipelineOrchestrator provides the necessary orchestration logic to allow multiple network services
 * to share a common OpenFlow 1.3 based multi-table pipeline.
 *
 * @author Dave Tucker
 * @author Madhu Venugopal
 */
public interface PipelineOrchestrator {
    Service getNextServiceInPipeline(Service service);
    AbstractServiceInstance getServiceInstance(Service service);
    void enqueue(Node node);
    void registerService(final ServiceReference ref, AbstractServiceInstance serviceInstance);
    void unregisterService(final ServiceReference ref);
}
