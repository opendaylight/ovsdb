/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.connection.IPluginInConnectionService;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.opendaylight.controller.sal.utils.INodeFactory;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OVSDB protocol plugin Activator
 *
 *
 */
public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some initializations
     * are done by the ComponentActivatorAbstractBase.
     * Here it registers the node Type
     *
     */
    @Override
    public void init() {
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     *
     */
    @Override
    public void destroy() {
        Node.NodeIDType.unRegisterIDType("OVS");
        NodeConnector.NodeConnectorIDType.unRegisterIDType("OVS");
    }
    @Override
    public Object[] getGlobalImplementations() {
        Object[] res = { ConnectionService.class, ConfigurationService.class, NodeFactory.class, NodeConnectorFactory.class, InventoryService.class };
        return res;
    }

    @Override
    public void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(ConfigurationService.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            c.setInterface(new String[] { IPluginInBridgeDomainConfigService.class.getName(),
                                          OVSDBConfigService.class.getName()}, props);

            c.add(createServiceDependency()
                    .setService(IConnectionServiceInternal.class)
                    .setCallbacks("setConnectionServiceInternal", "unsetConnectionServiceInternal")
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(InventoryServiceInternal.class)
                    .setCallbacks("setInventoryServiceInternal", "unsetInventoryServiceInternal")
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(IClusterGlobalServices.class)
                    .setCallbacks("setClusterServices", "unsetClusterServices")
                    .setRequired(false));
        }

        if (imp.equals(ConnectionService.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            c.setInterface(
                    new String[] {IPluginInConnectionService.class.getName(),
                                  IConnectionServiceInternal.class.getName()}, props);

            c.add(createServiceDependency()
                    .setService(InventoryServiceInternal.class)
                    .setCallbacks("setInventoryServiceInternal", "unsetInventoryServiceInternal")
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbConnection.class)
                    .setCallbacks("setOvsdbConnection", "unsetOvsdbConnection")
                    .setRequired(true));
        }

        if (imp.equals(InventoryService.class)) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            c.setInterface(
                    new String[] {IPluginInInventoryService.class.getName(),
                            InventoryServiceInternal.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(IPluginOutInventoryService.class, "(scope=Global)")
                    .setCallbacks("setPluginOutInventoryServices",
                            "unsetPluginOutInventoryServices")
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OVSDBConfigService.class)
                    .setCallbacks("setConfigService", "unsetConfigService")
                    .setRequired(false));

        }

        if (imp.equals(NodeFactory.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            props.put("protocolName", "OVS");
            c.setInterface(INodeFactory.class.getName(), props);
        }
        if (imp.equals(NodeConnectorFactory.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            props.put("protocolName", "OVS");
            c.setInterface(INodeConnectorFactory.class.getName(), props);
        }

    }
}
