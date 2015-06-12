/*
 * Copyright (c) 2015 Inocybe Technologies.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link PipelineOrchestratorImplTest}
 */
@PrepareForTest(ServiceHelper.class)
@RunWith(PowerMockRunner.class)
public class PipelineOrchestratorImplTest {
    @InjectMocks private PipelineOrchestratorImpl orchestrator;

    @Mock private ExecutorService eventHandler;
    @Mock private Southbound southbound;

    /***
     * Registers a mock service and verifies the registration by asking the
     * pipeline orchestrator to return the associated service from its internal
     * registry
     */
    @Test
    public void testRegisterAndUnregisterService() {
        Service service = Service.CLASSIFIER;
        ServiceReference<?> serviceReference = mock(ServiceReference.class);
        when(serviceReference.getProperty(anyString())).thenReturn(service);

        AbstractServiceInstance abstractServiceInstance = mock(AbstractServiceInstance.class);

        orchestrator.registerService(serviceReference, abstractServiceInstance);
        assertEquals("Error, registerService() service registration fails",
                abstractServiceInstance,
                orchestrator.getServiceInstance(service));

        orchestrator.unregisterService(serviceReference);
        assertNull("Error, unregisterService() didn't delete the service", orchestrator.getServiceInstance(service));
    }

    /**
     * Test method
     * {@link PipelineOrchestratorImplr#getNextServiceInPipeline(Service)}
     */
    @Test
    public void testGetNextServiceInPipeline() {

        assertEquals(orchestrator.getNextServiceInPipeline(Service.CLASSIFIER),
                Service.ARP_RESPONDER);
        assertEquals(
                orchestrator.getNextServiceInPipeline(Service.ARP_RESPONDER),
                Service.INBOUND_NAT);
        assertEquals(
                orchestrator.getNextServiceInPipeline(Service.INBOUND_NAT),
                Service.EGRESS_ACL);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.EGRESS_ACL),
                Service.LOAD_BALANCER);
        assertEquals(
                orchestrator.getNextServiceInPipeline(Service.LOAD_BALANCER),
                Service.ROUTING);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.ROUTING),
                Service.L3_FORWARDING);
        assertEquals(
                orchestrator.getNextServiceInPipeline(Service.L3_FORWARDING),
                Service.L2_REWRITE);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.L2_REWRITE),
                Service.INGRESS_ACL);
        assertEquals(
                orchestrator.getNextServiceInPipeline(Service.INGRESS_ACL),
                Service.OUTBOUND_NAT);
        assertEquals(
                orchestrator.getNextServiceInPipeline(Service.OUTBOUND_NAT),
                Service.L2_FORWARDING);
        assertNull(orchestrator.getNextServiceInPipeline(Service.L2_FORWARDING));
    }

  @Test
  public void testSetDependencies() throws Exception {
      NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
      Southbound southbound = mock(Southbound.class);

      PowerMockito.mockStatic(ServiceHelper.class);
      PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, orchestrator)).thenReturn(nodeCacheManager);
      PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, orchestrator)).thenReturn(southbound);

      orchestrator.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

//      assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
      assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
  }

  private Object getField(String fieldName) throws Exception {
      Field field = PipelineOrchestratorImpl.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(orchestrator);
  }
}
