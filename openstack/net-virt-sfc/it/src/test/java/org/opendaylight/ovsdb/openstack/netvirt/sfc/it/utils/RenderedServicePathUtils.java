/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.it.utils;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;

public class RenderedServicePathUtils extends AbstractUtils {

    public CreateRenderedPathInputBuilder createRenderedPathInputBuilder(
            CreateRenderedPathInputBuilder createRenderedPathInputBuilder, String sfpName, String rspName) {
        return createRenderedPathInputBuilder
                .setName(rspName)
                .setParentServiceFunctionPath(sfpName)
                .setSymmetric(false);
    }
}
