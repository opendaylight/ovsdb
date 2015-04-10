/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southbound.impl.rev141210;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.overlay.OverlayProvider;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;

public class SouthboundImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southbound.impl.rev141210.AbstractSouthboundImplModule {


    public SouthboundImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SouthboundImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southbound.impl.rev141210.SouthboundImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        SouthboundUtil.setInstanceIdentifierCodec(new InstanceIdentifierCodec(getSchemaServiceDependency(),
                getBindingNormalizedNodeSerializerDependency()));
        SouthboundProvider southboundProvider = new SouthboundProvider();
        OverlayProvider overlayProvider = new OverlayProvider();
        getBrokerDependency().registerProvider(southboundProvider);
        getBrokerDependency().registerProvider(overlayProvider);

        class ProviderAggregator implements BindingAwareProvider, java.lang.AutoCloseable {
            public SouthboundProvider southboundProvider;
            public OverlayProvider overlayProvider;

            public ProviderAggregator(OverlayProvider overlayProvider, SouthboundProvider southboundProvider) {
                this.southboundProvider = southboundProvider;
                this.overlayProvider = overlayProvider;
            }

            @Override
            public void close() throws Exception {
                this.overlayProvider.close();
                this.southboundProvider.close();
            }

            @Override
            public void onSessionInitiated(ProviderContext session) {
                this.overlayProvider.onSessionInitiated(session);
                this.southboundProvider.onSessionInitiated(session);
            }
        }
        return new ProviderAggregator(overlayProvider, southboundProvider);
    }

}
