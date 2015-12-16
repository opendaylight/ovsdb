/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.utils;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;

public class ServiceFunctionPathUtils extends AbstractUtils {
    public ServiceFunctionPathBuilder serviceFunctionPathBuilder(
            ServiceFunctionPathBuilder serviceFunctionPathBuilder,
            String sfpName, String sfcName, short startingIndex, Boolean symmetric) {

        return serviceFunctionPathBuilder
                .setSymmetric(symmetric)
                .setName(SfpName.getDefaultInstance(sfpName))
                .setServiceChainName(SfcName.getDefaultInstance(sfcName))
                .setStartingIndex(startingIndex);
    }

    public ServiceFunctionPathsBuilder serviceFunctionPathsBuilder(
            List<ServiceFunctionPath> serviceFunctionPathList) {

        return new ServiceFunctionPathsBuilder().setServiceFunctionPath(serviceFunctionPathList);
    }
}
