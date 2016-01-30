/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.it.utils;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorOptionsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionaryBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;

public class ServiceFunctionForwarderUtils extends AbstractUtils {
    public OvsOptionsBuilder ovsOptionsBuilder(OvsOptionsBuilder ovsOptionsBuilder, int port) {
        String flow = "flow";
        return ovsOptionsBuilder
                .setDstPort(String.valueOf(port))
                .setRemoteIp(flow)
                .setKey(flow)
                .setNsi(flow)
                .setNsp(flow)
                .setNshc1(flow)
                .setNshc2(flow)
                .setNshc3(flow)
                .setNshc4(flow);
    }

    public SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder(SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder,
                                                                 DataPlaneLocatorBuilder dataPlaneLocatorBuilder,
                                                                 String dplName) {
        SffOvsLocatorOptionsAugmentationBuilder sffOvsLocatorOptionsAugmentationBuilder =
                new SffOvsLocatorOptionsAugmentationBuilder();
        sffOvsLocatorOptionsAugmentationBuilder.setOvsOptions(
                ovsOptionsBuilder(new OvsOptionsBuilder(), 6633).build());

        return sffDataPlaneLocatorBuilder
                .setName(new SffDataPlaneLocatorName(dplName))
                .setDataPlaneLocator(dataPlaneLocatorBuilder.build())
                .addAugmentation(SffOvsLocatorOptionsAugmentation.class,
                        sffOvsLocatorOptionsAugmentationBuilder.build());
    }

    public SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder(
            SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder, String sffDplName, String sfDplName) {
        return sffSfDataPlaneLocatorBuilder
                .setSfDplName(SfDataPlaneLocatorName.getDefaultInstance(sfDplName))
                .setSffDplName(SffDataPlaneLocatorName.getDefaultInstance(sffDplName));
    }

    public ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder(
            ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder,
            SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder,
            String sfName) {

        return serviceFunctionDictionaryBuilder
                .setName(SfName.getDefaultInstance(sfName))
                .setSffSfDataPlaneLocator(sffSfDataPlaneLocatorBuilder.build());
    }

    public OvsBridgeBuilder ovsBridgeBuilder(OvsBridgeBuilder ovsBridgeBuilder, String bridgeName) {
        return ovsBridgeBuilder.setBridgeName(bridgeName);
    }

    public ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder(
            ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder,
            String sffName, String serviceNodeName, String bridgeName,
            List<SffDataPlaneLocator> sffDataPlaneLocatorList,
            List<ServiceFunctionDictionary> serviceFunctionDictionaryList) {

        SffOvsBridgeAugmentationBuilder sffOvsBridgeAugmentationBuilder = new SffOvsBridgeAugmentationBuilder();
        sffOvsBridgeAugmentationBuilder.setOvsBridge(ovsBridgeBuilder(new OvsBridgeBuilder(), bridgeName).build());

        return serviceFunctionForwarderBuilder
                .setName(new SffName(sffName))
                .setServiceNode(new SnName(serviceNodeName))
                .setServiceFunctionDictionary(serviceFunctionDictionaryList)
                .setSffDataPlaneLocator(sffDataPlaneLocatorList)
                .addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBridgeAugmentationBuilder.build());
    }

    public ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder(
            ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder,
            List<ServiceFunctionForwarder> serviceFunctionForwarderList) {
        return serviceFunctionForwardersBuilder.setServiceFunctionForwarder(serviceFunctionForwarderList);
    }

    public ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder(
            String sffName, String sffIp, int port, String sffDplName,
            String sfIp, String snName, String bridgeName, String sfName, String sfDplName) {

        DataPlaneLocatorBuilder dataPlaneLocatorBuilder =
                dataPlaneLocatorBuilder(new DataPlaneLocatorBuilder(), sffIp, port);
        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder =
                sffDataPlaneLocatorBuilder( new SffDataPlaneLocatorBuilder(), dataPlaneLocatorBuilder, sffDplName);
        List<SffDataPlaneLocator> sffDataPlaneLocatorList =
                list(new ArrayList<SffDataPlaneLocator>(), sffDataPlaneLocatorBuilder);

        SffSfDataPlaneLocatorBuilder sffSfDataPlaneLocatorBuilder =
                sffSfDataPlaneLocatorBuilder(new SffSfDataPlaneLocatorBuilder(), sffDplName, sfDplName);
        ServiceFunctionDictionaryBuilder serviceFunctionDictionaryBuilder =
                serviceFunctionDictionaryBuilder(new ServiceFunctionDictionaryBuilder(),
                        sffSfDataPlaneLocatorBuilder, sfName);
        List<ServiceFunctionDictionary> serviceFunctionDictionaryList =
                list(new ArrayList<ServiceFunctionDictionary>(), serviceFunctionDictionaryBuilder);

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder =
                serviceFunctionForwarderBuilder(
                        new ServiceFunctionForwarderBuilder(), sffName, snName, bridgeName,
                        sffDataPlaneLocatorList, serviceFunctionDictionaryList);
        return serviceFunctionForwarderBuilder;
    }
}
