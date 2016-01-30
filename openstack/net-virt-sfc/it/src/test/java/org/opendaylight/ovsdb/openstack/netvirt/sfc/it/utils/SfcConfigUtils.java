/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.it.utils;

import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfigBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcConfigUtils {
    public SfcOfRendererConfigBuilder sfcOfRendererConfigBuilder(SfcOfRendererConfigBuilder sfcOfRendererConfigBuilder,
                                                                 short tableOffset, short egressTable) {
        return sfcOfRendererConfigBuilder
                .setSfcOfTableOffset(tableOffset)
                .setSfcOfAppEgressTableOffset(egressTable);
    }

    public InstanceIdentifier<SfcOfRendererConfig> getPath() {
        return InstanceIdentifier.create(SfcOfRendererConfig.class);
    }
}
