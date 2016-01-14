/*
 * Copyright (c) 2015, 2016 Inocybe Technologies.  All rights reserved.
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
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for {@link PipelineOrchestratorImplTest}
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineOrchestratorImplTest {
    @InjectMocks private PipelineOrchestratorImpl orchestrator;

    @Mock private ExecutorService eventHandler;
    @Mock private Southbound southbound;

    /**
     * Test for method {@link PipelineOrchestratorImpl#getTableOffset()}
     */
    @Test
    public void testGetTableOffset() {
        short tableOffset = 0;
        assertEquals("tableOffset was not set", tableOffset, orchestrator.getTableOffset());
    }

    /**
     * Test for {@link PipelineOrchestratorImpl#getTable(Service)}
     */
    @Test
    public void testGetTableOffsetWithService() {
        assertEquals("tableOffset was not set", Service.CLASSIFIER.getTable(),
                orchestrator.getTable(Service.CLASSIFIER));
    }

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
     * {@link PipelineOrchestratorImpl#getNextServiceInPipeline(Service)}
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

      ServiceHelper.overrideGlobalInstance(NodeCacheManager.class, nodeCacheManager);
      ServiceHelper.overrideGlobalInstance(Southbound.class, southbound);

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
