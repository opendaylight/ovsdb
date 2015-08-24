/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InstanceIdentifierUtils {

    private InstanceIdentifierUtils() {
        throw new UnsupportedOperationException("Utility class should never be instantiated");
    }

    public static final InstanceIdentifier<AccessLists> createAccessListsPath () {
        return InstanceIdentifier.builder(AccessLists.class).build();
    }

    public static final InstanceIdentifier<ServiceFunctionChains> createServiceFunctionChainsPath () {
        return InstanceIdentifier.builder(ServiceFunctionChains.class).build();
    }

    public static final InstanceIdentifier<ServiceFunctionForwarder> createServiceFunctionForwarderPath () {
        return InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class).build();
    }

    public static final InstanceIdentifier<ServiceFunctionForwarder> createServiceFunctionForwarderPath (String name) {
        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(name);
        return InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey)
                .build();
    }

    public static final InstanceIdentifier<ServiceFunctionForwarders> createServiceFunctionForwardersPath () {
        return InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();
    }

    public static final InstanceIdentifier<ServiceFunction> createServiceFunctionPath (String name) {
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(name);
        return InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, serviceFunctionKey)
                .build();
    }

    public static final InstanceIdentifier<ServiceFunctionPaths> createServiceFunctionPathsPath () {
        return InstanceIdentifier.builder(ServiceFunctionPaths.class).build();
    }

    public static final InstanceIdentifier<ServiceFunctionPath> createServiceFunctionPathPath () {
        return InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class).build();
    }
}