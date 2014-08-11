/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovs.nx.sal.config.impl.rev140711;

import org.opendaylight.openflowplugin.extension.api.ExtensionConverterRegistrator;
import org.opendaylight.ovs.nx.sal.NiciraExtensionProvider;

public class ConfigurableNiciraExtensionProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovs.nx.sal.config.impl.rev140711.AbstractConfigurableNiciraExtensionProviderModule {
    public ConfigurableNiciraExtensionProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ConfigurableNiciraExtensionProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovs.nx.sal.config.impl.rev140711.ConfigurableNiciraExtensionProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NiciraExtensionProvider provider = new NiciraExtensionProvider();
        ExtensionConverterRegistrator registrator = getOpenflowPluginProviderDependency().getExtensionConverterRegistrator();
        provider.setExtensionConverterRegistrator(registrator);
        provider.registerConverters();
        return provider;
    }

}
