/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Sam Hague
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableNodeDataChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableNodeDataChangeListener.class);
    private ListenerRegistration<DataChangeListener> registration;

    public static final InstanceIdentifier<FlowCapableNode> createFlowCapableNodePath () {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class)
                .augmentation(FlowCapableNode.class)
                .build();
    }

    public FlowCapableNodeDataChangeListener (DataBroker dataBroker) {
        LOG.info("Registering FlowCapableNodeChangeListener");
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                createFlowCapableNodePath(), this, AsyncDataBroker.DataChangeScope.ONE);
    }

    @Override
    public void close () throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged (AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {

        LOG.debug("onDataChanged: {}", changes);
        for( Map.Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            InstanceIdentifier<?> iiD = created.getKey();
            LOG.debug(">>>>> created iiD: {} - first: {} - NodeKey: {}",
                    iiD,
                    iiD.firstIdentifierOf(Node.class),
                    iiD.firstKeyOf(Node.class, NodeKey.class).getId().getValue());

            PipelineOrchestrator pipelineOrchestrator =
                    (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, this);
            pipelineOrchestrator.enqueue(iiD.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
        }
    }
}
