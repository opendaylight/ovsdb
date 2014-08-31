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

import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PipelineOrchestratorImpl implements PipelineOrchestrator {

    private List<Service> staticPipeline = Lists.newArrayList(
                                                                Service.CLASSIFIER,
                                                                Service.ARP_RESPONDER,
                                                                Service.INBOUND_NAT,
                                                                Service.INGRESS_ACL,
                                                                Service.LOAD_BALANCER,
                                                                Service.ROUTING,
                                                                Service.L2_REWRITE,
                                                                Service.L2_FORWARDING,
                                                                Service.EGRESS_ACL,
                                                                Service.OUTBOUND_NAT
                                                              );
    Map<Service, AbstractServiceInstance> serviceRegistry = Maps.newConcurrentMap();

    public PipelineOrchestratorImpl() {
    }

    public void registerService(final ServiceReference ref, AbstractServiceInstance serviceInstance){
        Service service = (Service)ref.getProperty(AbstractServiceInstance.SERVICE_PROPERTY);
        serviceRegistry.put(service, serviceInstance);
    }

    public void unregisterService(final ServiceReference ref) {
        serviceRegistry.remove(ref.getProperty(AbstractServiceInstance.SERVICE_PROPERTY));
    }
    @Override
    public Service getNextServiceInPipeline(Service service) {
        int index = staticPipeline.indexOf(service);
        if (index >= staticPipeline.size() - 1) return null;
        return staticPipeline.get(index + 1);
    }

    @Override
    public AbstractServiceInstance getServiceInstance(Service service) {
        if (service == null) return null;
        return serviceRegistry.get(service);
    }
}
