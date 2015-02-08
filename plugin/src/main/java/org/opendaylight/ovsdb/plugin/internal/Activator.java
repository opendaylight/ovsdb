/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.opendaylight.controller.sal.utils.INodeFactory;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.NodeConnectorFactory;
import org.opendaylight.ovsdb.plugin.impl.NodeFactory;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OVSDB protocol plugin Activator
 *
 *
 */
public class Activator extends DependencyActivatorBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        logger.info("shague>>>>> init");
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");

        manager.add(createComponent()
                        .setInterface(OvsdbConfigurationService.class.getName(), props)
                        .setImplementation(ConfigurationServiceImpl.class)
                        .add(createServiceDependency()
                                        .setService(OvsdbConnectionService.class)
                                        .setRequired(true))
                        .add(createServiceDependency()
                                .setService(OvsdbInventoryService.class)
                                .setRequired(true)));

        Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
        manager.add(createComponent()
                        .setInterface(
                                new String[] {OvsdbConnectionService.class.getName(),
                                        OvsdbConnectionListener.class.getName()}, props2)
                        .setImplementation(ConnectionServiceImpl.class)
                        .add(createServiceDependency()
                                .setService(OvsdbInventoryService.class)
                                .setRequired(true))
                        .add(createServiceDependency()
                                .setService(OvsdbConnection.class)
                                .setRequired(true))
        );

        Dictionary<String, Object> props3 = new Hashtable<>();
        props3.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
        props3.put("scope", "Global");
        manager.add(createComponent()
                        .setInterface(
                                new String[]{IPluginInInventoryService.class.getName(),
                                        OvsdbInventoryService.class.getName()}, props3)
                        .setImplementation(InventoryServiceImpl.class)
                        .add(createServiceDependency()
                                .setService(IPluginOutInventoryService.class, "(scope=Global)")
                                .setCallbacks("setPluginOutInventoryServices",
                                        "unsetPluginOutInventoryServices")
                                .setRequired(true))
                        .add(createServiceDependency()
                                .setService(OvsdbInventoryListener.class)
                                .setCallbacks("listenerAdded", "listenerRemoved"))
                        .add(createServiceDependency()
                                .setService(OvsdbConfigurationService.class)
                                .setRequired(false)));

        Dictionary<String, Object> props4 = new Hashtable<String, Object>();
        props4.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
        props4.put("protocolName", "OVS");

        manager.add(createComponent()
                .setInterface(INodeFactory.class.getName(), props4)
                .setImplementation(NodeFactory.class));

        Dictionary<String, Object> props5 = new Hashtable<String, Object>();
        props5.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
        props5.put("protocolName", "OVS");

        manager.add(createComponent()
                .setInterface(INodeConnectorFactory.class.getName(), props5)
                .setImplementation(NodeConnectorFactory.class));
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        Node.NodeIDType.unRegisterIDType("OVS");
        NodeConnector.NodeConnectorIDType.unRegisterIDType("OVS");
    }
/*
    @Override
    public Object[] getGlobalImplementations() {
        Object[] res = { ConnectionServiceImpl.class, ConfigurationServiceImpl.class, NodeFactory.class, NodeConnectorFactory.class, InventoryServiceImpl.class };
        return res;
    }

    @Override
    public void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(ConfigurationServiceImpl.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            c.setInterface(new String[] { OvsdbConfigurationService.class.getName()}, props);

            c.add(createServiceDependency()
                    .setService(OvsdbConnectionService.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryService.class)
                    .setRequired(true));
        }

        if (imp.equals(ConnectionServiceImpl.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            c.setInterface(
                    new String[] {OvsdbConnectionService.class.getName(),
                                  OvsdbConnectionListener.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryService.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbConnection.class)
                    .setRequired(true));
        }

        if (imp.equals(InventoryServiceImpl.class)) {
            Dictionary<String, Object> props = new Hashtable<>();
            props.put(GlobalConstants.PROTOCOLPLUGINTYPE.toString(), "OVS");
            props.put("scope", "Global");
            c.setInterface(
                    new String[]{IPluginInInventoryService.class.getName(),
                                 OvsdbInventoryService.class.getName()}, props);
            c.add(createServiceDependency()
                          .setService(IPluginOutInventoryService.class, "(scope=Global)")
                          .setCallbacks("setPluginOutInventoryServices",
                                        "unsetPluginOutInventoryServices")
                          .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryListener.class)
                    .setCallbacks("listenerAdded", "listenerRemoved"));
            c.add(createServiceDependency()
                    .setService(OvsdbConfigurationService.class)
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
*/
}
