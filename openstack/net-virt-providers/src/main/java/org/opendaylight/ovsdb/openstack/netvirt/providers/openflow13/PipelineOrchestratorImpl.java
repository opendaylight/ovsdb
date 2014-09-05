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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class PipelineOrchestratorImpl implements PipelineOrchestrator, OpendaylightInventoryListener, TransactionChainListener {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOrchestratorImpl.class);
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
    private volatile MdsalConsumer mdsalConsumer;
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
        NotificationProviderService notificationService = mdsalConsumer.getNotificationService();
        if (notificationService != null) {
            notificationService.registerNotificationListener(this);
        }
    }
    public void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                try {
                    while (true) {
                        String nodeId = queue.take();
                        for (Service service : staticPipeline) {
                            AbstractServiceInstance serviceInstance = getServiceInstance(service);
                            if (!serviceInstance.isBridgeInPipeline(nodeId)) {
                                logger.debug("Bridge {} is not in pipeline", nodeId);
                                continue;
                            }

                            serviceInstance.programDefaultPipelineRule(nodeId);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Processing interrupted, terminating ", e);
                    e.printStackTrace();
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

    void enqueue(String nodeId) {
        try {
            queue.put(new String(nodeId));
        } catch (InterruptedException e) {
            logger.warn("Failed to enqueue operation {}", nodeId, e);
        }
    }


    /**
     * Process the Node update notification. Check for Openflow node and make sure if the bridge is part of the Pipeline before
     * programming the Pipeline specific flows.
     */
    @Override
    public void onNodeUpdated(NodeUpdated nodeUpdated) {
        NodeRef ref = nodeUpdated.getNodeRef();
        InstanceIdentifier<Node> identifier = (InstanceIdentifier<Node>) ref.getValue();
        logger.info("GOT NOTIFICATION FOR "+identifier.toString());
        final NodeKey key = identifier.firstKeyOf(Node.class, NodeKey.class);
        final String nodeId = key.getId().getValue();
        if (key != null && key.getId().getValue().contains("openflow")) {
            InstanceIdentifierBuilder<Node> builder = ((InstanceIdentifier<Node>) ref.getValue()).builder();
            InstanceIdentifierBuilder<FlowCapableNode> augmentation = builder.augmentation(FlowCapableNode.class);
            final InstanceIdentifier<FlowCapableNode> path = augmentation.build();
            BindingTransactionChain txChain = mdsalConsumer.getDataBroker().createTransactionChain(this);
            CheckedFuture readFuture = txChain.newReadWriteTransaction().read(LogicalDatastoreType.OPERATIONAL, path);
            Futures.addCallback(readFuture, new FutureCallback<Optional<? extends DataObject>>() {
                @Override
                public void onSuccess(Optional<? extends DataObject> optional) {
                    if (!optional.isPresent()) {
                        enqueue(nodeId);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.debug(String.format("Can't retrieve node data for node %s. Writing node data with table0.", nodeId));
                    enqueue(nodeId);
                }
            });
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        logger.error("Failed to export Flow Capable Inventory, Transaction {} failed.",transaction.getIdentifier(),cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
    }

    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNodeRemoved(NodeRemoved arg0) {
        // TODO Auto-generated method stub

    }

}
