/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hwvtepsouthbound.impl.rev150901;

import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundProvider;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.InstanceIdentifierCodec;

public class HwvtepSouthboundModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hwvtepsouthbound.impl.rev150901.AbstractHwvtepSouthboundModule {
    public HwvtepSouthboundModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HwvtepSouthboundModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hwvtepsouthbound.impl.rev150901.HwvtepSouthboundModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        HwvtepSouthboundUtil.setInstanceIdentifierCodec(new InstanceIdentifierCodec(getSchemaServiceDependency(),
                        getBindingNormalizedNodeSerializerDependency()));
        HwvtepSouthboundProvider provider = new HwvtepSouthboundProvider(getClusteringEntityOwnershipServiceDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
