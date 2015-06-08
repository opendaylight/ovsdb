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
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineOrchestratorImpl implements ConfigInterface, NodeCacheListener, PipelineOrchestrator {
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
    private volatile BlockingQueue<Node> queue;
    private ExecutorService eventHandler;
    private Southbound southbound;

    public PipelineOrchestratorImpl() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.queue = new LinkedBlockingQueue<Node>();
        logger.info("PipelineOrchestratorImpl constructor");
        start();
    }

    public void registerService(final ServiceReference ref, AbstractServiceInstance serviceInstance){
        Service service = (Service)ref.getProperty(AbstractServiceInstance.SERVICE_PROPERTY);
        logger.info("registerService {} - {}", serviceInstance, service);
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

    public void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                try {
                    while (true) {
                        Node node = queue.take();
                        /*
                         * Since we are hooking on OpendaylightInventoryListener and as observed in
                         * Bug 1997 multiple Threads trying to write to a same table at the same time
                         * causes programming issues. Hence delaying the programming by a second to
                         * avoid the clash. This hack/workaround should be removed once Bug 1997 is resolved.
                         */
                        logger.info(">>>>> dequeue: {}", node);
                        Thread.sleep(1000);
                        for (Service service : staticPipeline) {
                            AbstractServiceInstance serviceInstance = getServiceInstance(service);
                            //logger.info("pipeline: {} - {}", service, serviceInstance);
                            if (serviceInstance != null) {
                                if (southbound.getBridge(node) != null) {
                                    serviceInstance.programDefaultPipelineRule(node);
                                }
                            }
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
    public void enqueue(Node node) {
        logger.info(">>>>> enqueue: {}", node);
        try {
            queue.put(node);
        } catch (InterruptedException e) {
            logger.warn("Failed to enqueue operation {}", node, e);
        }
    }

    @Override
    public void notifyNode(Node node, Action action) {
        if (action == Action.ADD) {
            enqueue(node);
        } else {
            logger.debug("update ignored: {} action {}", node.getNodeId().getValue(), action);
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        NodeCacheManager nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        nodeCacheManager.cacheListenerAdded(
                bundleContext.getServiceReference(PipelineOrchestrator.class.getName()), this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
