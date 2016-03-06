/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.NetvirtProvidersProvider;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PipelineOrchestratorImpl implements ConfigInterface, NodeCacheListener, PipelineOrchestrator {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineOrchestratorImpl.class);

    /**
     * Return the current table offset
     * @return The table offset
     */
    @Override
    public short getTableOffset() {
        return NetvirtProvidersProvider.getTableOffset();
    }

    /**
     * Return the offset adjusted table for the given {@link Service}
     * @param service Identifies the openflow {@link Service}
     * @return The table id
     */
    @Override
    public short getTable(Service service) {
        return (short)(getTableOffset() + service.getTable());
    }

    public List<Service> getStaticPipeline() {
        return staticPipeline;
    }

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

    public Map<Service, AbstractServiceInstance> getServiceRegistry() {
        return serviceRegistry;
    }

    Map<Service, AbstractServiceInstance> serviceRegistry = Maps.newConcurrentMap();
    private volatile BlockingQueue<Node> queue;
    private ExecutorService eventHandler;
    private Southbound southbound;

    public PipelineOrchestratorImpl() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.queue = new LinkedBlockingQueue<>();
        LOG.info("PipelineOrchestratorImpl constructor");
        start();
    }

    public void registerService(final ServiceReference ref, AbstractServiceInstance serviceInstance){
        Service service = (Service)ref.getProperty(AbstractServiceInstance.SERVICE_PROPERTY);
        LOG.info("registerService {} - {}", serviceInstance, service);
        serviceRegistry.put(service, serviceInstance);
        // insert the service if not already there. The list is ordered based of table ID.
        if (!staticPipeline.contains(service) && !isTableInPipeline(service.getTable())) {
            staticPipeline.add(service);
            Collections.sort(staticPipeline, Service.insertComparator);
        }
        LOG.info("registerService: {}", staticPipeline);
    }

    private boolean isTableInPipeline (short tableId) {
        boolean found = false;
        for (Service service : staticPipeline) {
            if (service.getTable() == tableId) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void unregisterService(final ServiceReference ref) {
        serviceRegistry.remove(ref.getProperty(AbstractServiceInstance.SERVICE_PROPERTY));
    }
    @Override
    public Service getNextServiceInPipeline(Service service) {
        int index = staticPipeline.indexOf(service);
        if (index >= staticPipeline.size() - 1) {
            return null;
        }
        return staticPipeline.get(index + 1);
    }

    @Override
    public AbstractServiceInstance getServiceInstance(Service service) {
        if (service == null) {
            return null;
        }
        return serviceRegistry.get(service);
    }

    public final void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                try {
                    while (true) {
                        Node node = queue.take();
                        LOG.info(">>>>> dequeue: {}", node);
                        if (southbound.getBridge(node) != null) {
                            for (Service service : staticPipeline) {
                                AbstractServiceInstance serviceInstance = getServiceInstance(service);
                                if (serviceInstance != null) {
                                    serviceInstance.programDefaultPipelineRule(node);
                                }
                            }
                            // TODO: might need a flow to go from table 0 to the pipeline
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Processing interrupted, terminating ", e);
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
        LOG.info(">>>>> enqueue: {}", node);
        try {
            queue.put(node);
        } catch (InterruptedException e) {
            LOG.warn("Failed to enqueue operation {}", node, e);
        }
    }

    @Override
    public void notifyNode(Node node, Action action) {
        if (action == Action.ADD) {
            enqueue(node);
        } else {
            LOG.info("update ignored: {}", node);
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
