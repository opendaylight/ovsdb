package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class PipelineOrchestratorTest {
    PipelineOrchestrator orchestrator;
    @Before
    public void initialize() {
        orchestrator = new PipelineOrchestratorImpl();
    }

    @Test
    public void testPipeline() {
        assertEquals(orchestrator.getNextServiceInPipeline(Service.CLASSIFIER), Service.DIRECTOR);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.DIRECTOR), Service.ARP_RESPONDER);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.ARP_RESPONDER), Service.INBOUND_NAT);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.INBOUND_NAT), Service.INGRESS_ACL);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.INGRESS_ACL), Service.LOAD_BALANCER);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.LOAD_BALANCER), Service.ROUTING);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.ROUTING), Service.L2_REWRITE);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.L2_REWRITE), Service.L2_FORWARDING);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.L2_FORWARDING), Service.EGRESS_ACL);
        assertEquals(orchestrator.getNextServiceInPipeline(Service.EGRESS_ACL), Service.OUTBOUND_NAT);
        assertNull(orchestrator.getNextServiceInPipeline(Service.OUTBOUND_NAT));
    }
}
