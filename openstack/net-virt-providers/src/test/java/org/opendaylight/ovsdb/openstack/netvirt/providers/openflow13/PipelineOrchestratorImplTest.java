/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronLoadBalancerPool;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.LBaaSHandler;
import org.opendaylight.ovsdb.openstack.netvirt.LBaaSPoolHandler;
import org.opendaylight.ovsdb.openstack.netvirt.NeutronCacheUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.impl.EventDispatcherImpl;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link PipelineOrchestratorImplTest}
 */
@PrepareForTest(PipelineOrchestratorImpl.class)
@RunWith(PowerMockRunner.class)
public class PipelineOrchestratorImplTest {


	@Mock private ServiceReference ref;
	@Mock private ServiceReference ref2;
	@Mock private AbstractServiceInstance serviceInstance;
	@Mock private AbstractServiceInstance serviceInstance2;

	@InjectMocks private PipelineOrchestratorImpl orchestrator;

    private AbstractEvent.HandlerType handlerTypeObject = AbstractEvent.HandlerType.NEUTRON_FLOATING_IP;


	@Before
    public void setUp() {
		Random r = new Random();

		orchestrator = new PipelineOrchestratorImpl();
		orchestrator.init();
		orchestrator.start();

        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());
        when(ref.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY)).thenReturn(handlerTypeObject);
        when(ref.getProperty(AbstractServiceInstance.SERVICE_PROPERTY)).thenReturn(Service.CLASSIFIER);

        when(ref2.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());
        when(ref2.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY)).thenReturn(handlerTypeObject);
        when(ref2.getProperty(AbstractServiceInstance.SERVICE_PROPERTY)).thenReturn(Service.INBOUND_NAT);


        when(serviceInstance.getService()).thenReturn(Service.CLASSIFIER);
        when(serviceInstance2.getService()).thenReturn(Service.INBOUND_NAT);
    }

	/***
	 * Registers a mock service and verifies the registration
	 * by asking the pipeline orchestrator to return the associated service
	 * from its internal registry
	 */
	@Test
	public void testRegisterService(){
		orchestrator.registerService(ref, serviceInstance);
		assertEquals("Error, registerService() service registration fails"
				, serviceInstance
				, orchestrator.getServiceInstance(Service.CLASSIFIER));
	}

	/***
	 * Test method {@link PipelineOrchestratorImplr#registerService(Service)}
	 *
	 * Unregisters a mock service and verifies the process
	 * by asking the pipeline orchestrator to return the associated service
	 * from its internal registry
	 */
	@Test
	public void testUnRegisterService(){

		orchestrator = new PipelineOrchestratorImpl();
		orchestrator.init();
		orchestrator.start();
		orchestrator.registerService(ref, serviceInstance);
		orchestrator.unregisterService(ref);

		assertEquals("Error, unregisterService() service registration fails"
				, null
				, orchestrator.getServiceInstance(Service.CLASSIFIER));

	}

	/**
     * Test method {@link PipelineOrchestratorImplr#getNextServiceInPipeline(Service)}
     */

	@Test
	public void testGetNextServiceInPipeline(){

		assertEquals(orchestrator.getNextServiceInPipeline(Service.CLASSIFIER), Service.ARP_RESPONDER);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.ARP_RESPONDER), Service.INBOUND_NAT);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.INBOUND_NAT), Service.EGRESS_ACL);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.EGRESS_ACL), Service.LOAD_BALANCER);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.LOAD_BALANCER), Service.ROUTING);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.ROUTING), Service.L3_FORWARDING);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.L3_FORWARDING), Service.L2_REWRITE);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.L2_REWRITE), Service.INGRESS_ACL);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.INGRESS_ACL), Service.OUTBOUND_NAT);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.OUTBOUND_NAT), Service.L2_FORWARDING);
        assertNull(orchestrator.getNextServiceInPipeline(Service.L2_FORWARDING));

	}

	/**
     * Test method {@link PipelineOrchestratorImpl#getServiceInstance(Service)}
     */
	@Test
	public void testGetServiceInstance(){

		orchestrator = new PipelineOrchestratorImpl();
		orchestrator.init();
		orchestrator.start();
		orchestrator.registerService(ref, serviceInstance);
		orchestrator.registerService(ref2,serviceInstance2);

		assertEquals("Error, getServiceInstance() fails to return an instance of a registered service"
				, serviceInstance
				, orchestrator.getServiceInstance(Service.CLASSIFIER));

		assertEquals("Error, getServiceInstance() returned an instance of a service that wasn't registered."
				, null
				, orchestrator.getServiceInstance(Service.DIRECTOR));
	}


}
