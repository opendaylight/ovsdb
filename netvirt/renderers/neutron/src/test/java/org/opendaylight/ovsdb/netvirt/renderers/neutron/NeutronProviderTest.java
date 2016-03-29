/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;

import static org.mockito.Mockito.mock;

public class NeutronProviderTest {
    @Test
    public void testOnSessionInitiated() {

        NeutronProvider provider = new NeutronProvider();

        // ensure no exceptions
        // currently this method is empty

        //TODO
        //provider.onSessionInitiated(mock(BindingAwareBroker.ProviderContext.class));
    }

    @Test
    public void testClose() throws Exception {
        NeutronProvider provider = new NeutronProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }
}
