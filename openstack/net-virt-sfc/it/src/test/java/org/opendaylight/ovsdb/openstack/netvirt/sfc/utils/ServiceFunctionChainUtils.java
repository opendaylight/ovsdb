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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;

public class ServiceFunctionChainUtils extends AbstractUtils {
    public SfcServiceFunctionBuilder sfcServiceFunctionBuilder(SfcServiceFunctionBuilder sfcServiceFunctionBuilder,
                                                               String name,
                                                               SftType type) {
        return sfcServiceFunctionBuilder
                .setName(name)
                .setType(type);
    }

    public ServiceFunctionChainBuilder serviceFunctionChainBuilder(
            ServiceFunctionChainBuilder serviceFunctionChainBuilder, String name, Boolean symmetric,
            List<SfcServiceFunction> sfcServiceFunctionList) {

        return serviceFunctionChainBuilder
                .setName(SfcName.getDefaultInstance(name))
                .setSymmetric(symmetric)
                .setSfcServiceFunction(sfcServiceFunctionList);
    }

    public ServiceFunctionChainsBuilder serviceFunctionChainsBuilder(
            ServiceFunctionChainsBuilder serviceFunctionChainsBuilder,
            List<ServiceFunctionChain> serviceFunctionChainBuilderList) {

        return serviceFunctionChainsBuilder.setServiceFunctionChain(serviceFunctionChainBuilderList);
    }
}
