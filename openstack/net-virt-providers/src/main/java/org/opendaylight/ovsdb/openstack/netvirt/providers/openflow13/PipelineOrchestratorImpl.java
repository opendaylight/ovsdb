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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineOrchestratorImpl implements PipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOrchestratorImpl.class);
    private List<Service> staticPipeline = Lists.newArrayList(
                                                                Service.CLASSIFIER,
                                                                Service.ARP_RESPONDER,
                                                                Service.INBOUND_NAT,
                                                                Service.EGRESS_ACL,
                                                                Service.LOAD_BALANCER,
                                                                Service.ROUTING,
                                                                Service.L3_FORWARDING,
                                                                Service.L2_REWRITE,
                                                                Service.INGRESS_ACL,
                                                                Service.OUTBOUND_NAT,
                                                                Service.L2_FORWARDING
                                                              );
    Map<Service, AbstractServiceInstance> serviceRegistry = Maps.newConcurrentMap();
    private volatile BlockingQueue<String> queue;
    private ExecutorService eventHandler;
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

    public void init() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.queue = new LinkedBlockingQueue<String>();
    }

    public void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                try {
                    while (true) {
                        String nodeId = queue.take();
                        /*
                         * Since we are hooking on OpendaylightInventoryListener and as observed in
                         * Bug 1997 multiple Threads trying to write to a same table at the same time
                         * causes programming issues. Hence delaying the programming by a second to
                         * avoid the clash. This hack/workaround should be removed once Bug 1997 is resolved.
                         */
                        logger.info(">>>>> dequeue: {}", nodeId);
                        Thread.sleep(1000);
                        for (Service service : staticPipeline) {
                            AbstractServiceInstance serviceInstance = getServiceInstance(service);
                            serviceInstance.programDefaultPipelineRule(nodeId);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Processing interrupted, terminating ", e);
                }

                while (!queue.isEmpty()) {
                    queue.poll();
                }
                queue = null;
            }
        });
    }

    public void stop() {
        queue.clear();
        eventHandler.shutdownNow();
    }

    @Override
    public void enqueue(String nodeId) {
        logger.info(">>>>> enqueue: {}", nodeId);
        try {
            queue.put(new String(nodeId));
        } catch (InterruptedException e) {
            logger.warn("Failed to enqueue operation {}", nodeId, e);
        }
    }
}
