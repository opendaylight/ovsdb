/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.net.HttpURLConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.opendaylight.neutron.spi.NeutronFirewall;
import org.opendaylight.neutron.spi.NeutronFirewallPolicy;
import org.opendaylight.neutron.spi.NeutronFirewallRule;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * Unit test for {@link FWaasHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class FWaasHandlerTest {

    @InjectMocks FWaasHandler fwaasHandler;

    @Before
    public void setUp() {
        fwaasHandler = mock(FWaasHandler.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testCanCreateNeutronFirewall(){
        assertEquals("Error, canCreateNeutronFirewall() did not return the correct value ", HttpURLConnection.HTTP_CREATED, fwaasHandler.canCreateNeutronFirewall(any(NeutronFirewall.class)));
    }

    @Test
    public void testNeutronFirewallCreated(){
        verifyNoMoreInteractions(fwaasHandler);
        fwaasHandler.canCreateNeutronFirewall(mock(NeutronFirewall.class));
        verify(fwaasHandler, times(1)).canCreateNeutronFirewall(any(NeutronFirewall.class));
    }

    @Test
    public void testCanUpdateNeutronFirewall(){
        assertEquals("Error, canUpdateNeutronFirewall() did not return the correct value ", HttpURLConnection.HTTP_OK, fwaasHandler.canUpdateNeutronFirewall(any(NeutronFirewall.class), any(NeutronFirewall.class)));
    }

    @Test
    public void testCanDeleteNeutronFirewall(){
        assertEquals("Error, canDeleteNeutronFirewall() did not return the correct value ", HttpURLConnection.HTTP_OK, fwaasHandler.canDeleteNeutronFirewall(any(NeutronFirewall.class)));
    }

    @Test
    public void testNeutronFirewallDeleted(){
        verifyNoMoreInteractions(fwaasHandler);
        fwaasHandler.neutronFirewallDeleted(mock(NeutronFirewall.class));
        verify(fwaasHandler, times(1)).canDeleteNeutronFirewall(any(NeutronFirewall.class));
    }

    @Test
    public void testCanCreateNeutronFirewallRule(){
        assertEquals("Error, canCreateNeutronFirewallRule() did not return the correct value ", HttpURLConnection.HTTP_CREATED, fwaasHandler.canCreateNeutronFirewallRule(any(NeutronFirewallRule.class)));
    }

    @Test
    public void testNeutronFirewallRuleCreated(){
        verifyNoMoreInteractions(fwaasHandler);
        fwaasHandler.neutronFirewallRuleCreated(mock(NeutronFirewallRule.class));
        verify(fwaasHandler, times(1)).canCreateNeutronFirewallRule(any(NeutronFirewallRule.class));
    }

    @Test
    public void testCanUpdateNeutronFirewallRule(){
        assertEquals("Error, canUpdateNeutronFirewallRule() did not return the correct value ", HttpURLConnection.HTTP_OK, fwaasHandler.canUpdateNeutronFirewallRule(any(NeutronFirewallRule.class), any(NeutronFirewallRule.class)));
    }

    @Test
    public void testCanDeleteNeutronFirewallRule(){
        assertEquals("Error, canDeleteNeutronFirewallRule() did not return the correct value ", HttpURLConnection.HTTP_OK, fwaasHandler.canDeleteNeutronFirewallRule(any(NeutronFirewallRule.class)));
    }

    @Test
    public void testNeutronFirewallRuleDeleted(){
        verifyNoMoreInteractions(fwaasHandler);
        fwaasHandler.neutronFirewallRuleDeleted(mock(NeutronFirewallRule.class));
        verify(fwaasHandler, times(1)).canDeleteNeutronFirewallRule(any(NeutronFirewallRule.class));
    }

    @Test
    public void testCanCreateNeutronFirewallPolicy(){
        assertEquals("Error, canCreateNeutronFirewallPolicy() did not return the correct value ", HttpURLConnection.HTTP_CREATED, fwaasHandler.canCreateNeutronFirewallPolicy(any(NeutronFirewallPolicy.class)));
    }

    @Test
    public void testNeutronFirewallPolicyCreated(){
        verifyNoMoreInteractions(fwaasHandler);
        fwaasHandler.neutronFirewallPolicyCreated(mock(NeutronFirewallPolicy.class));
        verify(fwaasHandler, times(1)).canCreateNeutronFirewallPolicy(any(NeutronFirewallPolicy.class));
    }

    @Test
    public void testCanUpdateNeutronFirewallPolicy(){
        assertEquals("Error, canUpdateNeutronFirewallPolicy() did not return the correct value ", HttpURLConnection.HTTP_OK, fwaasHandler.canUpdateNeutronFirewallPolicy(any(NeutronFirewallPolicy.class), any(NeutronFirewallPolicy.class)));
    }

    @Test
    public void testCanDeleteNeutronFirewallPolicy(){
        assertEquals("Error, canDeleteNeutronFirewallPolicy() did not return the correct value ", HttpURLConnection.HTTP_OK, fwaasHandler.canDeleteNeutronFirewallPolicy(any(NeutronFirewallPolicy.class)));
    }

    @Test
    public void testNeutronFirewallPolicyDeleted(){
        verifyNoMoreInteractions(fwaasHandler);
        fwaasHandler.neutronFirewallPolicyDeleted(mock(NeutronFirewallPolicy.class));
        verify(fwaasHandler, times(1)).canDeleteNeutronFirewallPolicy(any(NeutronFirewallPolicy.class));
    }

    @Test
    public void testProcessEvent(){
        // TODO
        // no yet implemented
//        NorthboundEvent ev = mock(NorthboundEvent.class);
//        when(ev.getAction()).thenReturn(Action.ADD);
//        fwaasHandler.processEvent(ev);
    }

    @Test
    public void testSetDependencies() {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, fwaasHandler)).thenReturn(eventDispatcher);

        fwaasHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", fwaasHandler.eventDispatcher, eventDispatcher);
    }
}
