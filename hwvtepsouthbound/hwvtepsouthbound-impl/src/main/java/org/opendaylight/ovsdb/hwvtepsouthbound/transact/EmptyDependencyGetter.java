/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collections;
import java.util.List;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class EmptyDependencyGetter extends UnMetDependencyGetter {

    public static final EmptyDependencyGetter INSTANCE = new EmptyDependencyGetter();

    private EmptyDependencyGetter() {
    }

    @Override
    public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(EntryObject data) {
        return Collections.emptyList();
    }

    @Override
    public List<InstanceIdentifier<?>> getTerminationPointDependencies(EntryObject data) {
        return Collections.emptyList();
    }
}
