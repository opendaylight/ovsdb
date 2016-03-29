/*
 * Copyright (c) 2015 Red Hat Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalHelper.class);
    public static InstanceIdentifier<Port> createInstanceIdentifier(Uuid portUuid) {
        return InstanceIdentifier.create(Ports.class)
                .child(Port.class, new PortKey(portUuid));
    }
}
