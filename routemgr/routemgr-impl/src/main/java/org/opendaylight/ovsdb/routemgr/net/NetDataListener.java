/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;

//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Registration;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.subnets.Subnet;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.Port;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;

public class NetDataListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(NetDataListener.class);
    private final DataBroker dataService;

    //private static final String CURRTOPO = "router:1";
    private Registration rtrListener;
    private Registration portListener;
    private Registration subnetListener;

    public NetDataListener(DataBroker dataBroker) {
        this.dataService = dataBroker;
    }

    public void registerDataChangeListener() {
        LOG.debug("registering as listener for router, subnet and port");
        InstanceIdentifier<Router> rtrInstance = InstanceIdentifier.builder(Neutron.class)
                .child(Routers.class)
                .child(Router.class) // , new RouterKey(new Uuid(CURRTOPO)))
                .build();
        rtrListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, rtrInstance,
                                                     this, AsyncDataBroker.DataChangeScope.BASE);
        InstanceIdentifier<Subnet> subnetInstance = InstanceIdentifier.builder(Neutron.class)
                .child(Subnets.class)
                .child(Subnet.class) //, new SubnetKey(new Uuid("subnet:1")))
                .build();

        subnetListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, subnetInstance,
                                                             this, AsyncDataBroker.DataChangeScope.BASE);
        InstanceIdentifier<Port> portInstance = InstanceIdentifier.builder(Neutron.class)
                .child(Ports.class)
                .child(Port.class) //, new PortKey(new Uuid("port:1")))
                .build();

        portListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, portInstance,
                this, AsyncDataBroker.DataChangeScope.BASE);
        LOG.debug("registered");
        return;
    }
    public void closeListeners() throws Exception  {
        if (rtrListener != null) {
            rtrListener.close();
        }
        if (subnetListener != null) {
            subnetListener.close();
        }
        if (portListener != null) {
            portListener.close();
        }

        return;
    }


    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
        if (dataChangeEvent == null) {
            return;
        }
        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> updatedData = dataChangeEvent.getUpdatedData();
        Set<InstanceIdentifier<?>> removedData = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
        if ((createdData != null) && !(createdData.isEmpty())) {
            Set<InstanceIdentifier<?>> createdSet = createdData.keySet();
            for (InstanceIdentifier<?> instanceId : createdSet) {
                processInstanceId(instanceId, createdData.get(instanceId), true);
            }
        }
        if ((updatedData != null) && !(updatedData.isEmpty())) {
            Set<InstanceIdentifier<?>> updatedSet = updatedData.keySet();
            for (InstanceIdentifier<?> instanceId : updatedSet) {
                processInstanceId(instanceId, updatedData.get(instanceId), true);
            }
        }
        if ((removedData != null) && (!removedData.isEmpty()) && (originalData != null) && (!originalData.isEmpty())) {
            for (InstanceIdentifier<?> instanceId : removedData) {
                processInstanceId(instanceId, originalData.get(instanceId), false);
            }
        }
    }

    public void readDataStore() {
        // TODO:: Need to perform initial data store read
    }

    private void processInstanceId(InstanceIdentifier instanceId, DataObject data, boolean updDelFlag) {
        LOG.debug("entering processInstanceId");
        if (instanceId.getTargetType().equals(Router.class)) {
            Router rtr = (Router)data;
            LOG.debug("processing router up/down events");
            if (updDelFlag == true) {
                addRouter(rtr);
            } else {
                removeRouter(rtr);
            }
        } else if (instanceId.getTargetType().equals(Subnet.class)) {
            Subnet snet = (Subnet)data;
            LOG.debug("processing subnet up/down events");
            if (updDelFlag == true) {
                addSubnet(snet);
            } else {
                removeSubnet(snet);
            }
        } else if (instanceId.getTargetType().equals(Port.class)) {
            Port port = (Port)data;
            LOG.debug("processing port up/down events");
            if (updDelFlag == true) {
                addPort(port);
            } else {
                removePort(port);
            }
        }
    }

    private void addRouter(Router rtr) {
        LOG.debug("added neutron router {}", rtr);
        return;
    }

    private void removeRouter(Router rtr) {
        LOG.debug("remove neutron router {}", rtr);
        return;
    }

    private void addSubnet(Subnet snet) {
        LOG.debug("added neutron subnet {}", snet);
        return;
    }

    private void removeSubnet(Subnet snet) {
        LOG.debug("remove neutron subnet {}", snet);
        return;
    }

    private void addPort(Port port) {
        LOG.debug("added neutron port {}", port);
        return;
    }

    private void removePort(Port port) {
        LOG.debug("remove neutron port {}", port);
        return;
    }
}
