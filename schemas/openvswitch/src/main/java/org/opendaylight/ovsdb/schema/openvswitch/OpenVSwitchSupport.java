/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import org.opendaylight.ovsdb.lib.schema.typed.MethodDispatch;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

/**
 * Utility class for working with OpenVSwitch schema.
 */
/*
 * TODO: fun project: This class should be generated with a javax.annotation.processing.Processor.
 *
 * This would ensure the METHOD_DISPATCH map is generated completely from the contents of this package, and we could
 * actually generate access interfaces which have to be provided at runtime -- i.e. all the proxying stuff done through
 * TypedDatabaseSchema would be backed by compile-time-generated interfaces against which a client can be coded.
 *
 * Yes, it sounds like MD-SAL. It is like MD-SAL, but the data model is expressed in annotated classes and it is bound
 * loosely at runtime.
 */
// FIXME: this should be implementing an interface and have a global instance
public final class OpenVSwitchSupport {
    private static final ImmutableMap<Class<? extends TypedBaseTable<?>>, MethodDispatch> METHOD_DISPATCH;

    static {
        // Note keep this list up to date with all classes defined in the schema
        final List<Class<? extends TypedBaseTable<?>>> tableTypes = ImmutableList.of(
            AutoAttach.class,
            Bridge.class,
            Capability.class,
            Controller.class,
            FlowSampleCollectorSet.class,
            FlowTable.class,
            Interface.class,
            IPFIX.class,
            Manager.class,
            Mirror.class,
            NetFlow.class,
            OpenVSwitch.class,
            Port.class,
            Qos.class,
            Queue.class,
            SFlow.class,
            SSL.class);

        final Builder<Class<? extends TypedBaseTable<?>>, MethodDispatch> builder =
                ImmutableMap.builderWithExpectedSize(tableTypes.size());
        for (Class<? extends TypedBaseTable<?>> tableType : tableTypes) {
            builder.put(tableType, MethodDispatch.forTarget(tableType));
        }
        METHOD_DISPATCH = builder.build();
    }

    private OpenVSwitchSupport() {
        // Hidden on purpose
    }
}
