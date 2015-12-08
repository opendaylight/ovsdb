/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.neutron.utils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronModelsDataStoreHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);
    private DataBroker databroker = null;
    private MdsalUtils mdsalClient = null;

    /**
     * Class constructor setting the data broker.
     *
     * @param dataBroker the {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     */
    public NeutronModelsDataStoreHelper(DataBroker dataBroker) {
        this.databroker = dataBroker;
        this.mdsalClient = new MdsalUtils(this.databroker);
    }

    public Routers readAllNeutronRouters() {
        Routers routers = this.mdsalClient.read(LogicalDatastoreType.CONFIGURATION, getNeutrounRoutersPath());
        if(routers != null ) {
            LOG.debug("{} routers present in data store", routers.getRouter()!=null? routers.getRouter().size():0);
        }
        return routers;
    }

    public Ports readAllNeutronPorts() {
        Ports ports = this.mdsalClient.read(LogicalDatastoreType.CONFIGURATION, getNeutrounPortsPath());
        if(ports != null ) {
            LOG.debug("{} ports present in data store", ports.getPort()!=null? ports.getPort().size():0);
        }
        return ports;
    }
    public Port readNeutronPort(Uuid portId) {
        Port mdsalPort = this.mdsalClient.read(LogicalDatastoreType.CONFIGURATION, getNeutronPortPath(portId));
        if (mdsalPort != null) {
            LOG.debug("Port {} fetched from config data store for router interface {}",mdsalPort, portId);
        }
        return mdsalPort;
    }

    private InstanceIdentifier<Routers> getNeutrounRoutersPath() {
        return InstanceIdentifier
                .create(Neutron.class)
                .child(Routers.class);
    }

    private InstanceIdentifier<Ports> getNeutrounPortsPath() {
        return InstanceIdentifier
                .create(Neutron.class)
                .child(Ports.class);
    }

    private InstanceIdentifier<Port> getNeutronPortPath(Uuid portId) {
        return InstanceIdentifier
                .create(Neutron.class)
                .child(Ports.class)
                .child(Port.class,new PortKey(portId));
    }
}
