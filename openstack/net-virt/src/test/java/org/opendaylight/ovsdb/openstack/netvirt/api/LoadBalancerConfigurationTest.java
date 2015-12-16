/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Marcus Koontz
*/
package org.opendaylight.ovsdb.openstack.netvirt.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class LoadBalancerConfigurationTest {
    private LoadBalancerConfiguration lbConfig;
    private Map<String, LoadBalancerConfiguration.LoadBalancerPoolMember> lbMap = new HashMap<>();
    private Object[][] valuesArray =
            {{"mockUUID1", "mockUUID2", "mockUUID3"},
            {"10.10.1.343", "10.10.1.391", "10.10.1.31"},
            {"D5:6B:59:E8:F4:84", "D5:4B:60:E8:F5:84", "D5:4B:60:E8:F6:84"},
            {"tcp", "tcp", "http"},
            {125304, 125304, 1204}};

    @Before
    public void setUp() throws Exception {
        lbConfig = new LoadBalancerConfiguration();
        lbConfig.addMember(valuesArray[0][0].toString(), valuesArray[1][0].toString(),
                valuesArray[2][0].toString(), valuesArray[3][0].toString(), (Integer)valuesArray[4][0]);
        lbConfig.addMember(valuesArray[0][1].toString(), valuesArray[1][1].toString(),
                valuesArray[2][1].toString(), valuesArray[3][1].toString(), (Integer)valuesArray[4][1]);
        lbMap = lbConfig.getMembers();
    }

    @Test
    public void testConstructorLbConfig() throws Exception{
        lbConfig.setName("test-name");
        lbConfig.setProviderNetworkType("vlan");
        lbConfig.setProviderSegmentationId("segment-id-mock");
        lbConfig.setVip("192.168.58.87");
        lbConfig.setVmac("A2-E2-24-E3-44-8D");
        LoadBalancerConfiguration lbConfigLocal = new LoadBalancerConfiguration(lbConfig);

        assertEquals("getName() returned the wrong value", "test-name", lbConfigLocal.getName());
        assertEquals("getProviderNetworkType() returned the wrong value", "vlan",
                lbConfigLocal.getProviderNetworkType());
        assertEquals("getProviderSegmentationId() returned the wrong value", "segment-id-mock",
                lbConfigLocal.getProviderSegmentationId());
        assertEquals("getVip() returned the wrong value", "192.168.58.87", lbConfigLocal.getVip());
        assertEquals("getVmac() returned the wrong value", "A2-E2-24-E3-44-8D", lbConfigLocal.getVmac());

        Map<String, LoadBalancerConfiguration.LoadBalancerPoolMember> lbMapLocal = lbConfigLocal.getMembers();
        for (LoadBalancerConfiguration.LoadBalancerPoolMember lbMember: lbMapLocal.values()){
            assertTrue("IP expected " + valuesArray[1][lbMember.getIndex()] + " found: " +
                    lbMember.getIP(), valuesArray[1][lbMember.getIndex()] == lbMember.getIP());
            assertTrue("MAC expected " + valuesArray[2][lbMember.getIndex()] + " found " +
                    lbMember.getMAC(), valuesArray[2][lbMember.getIndex()] == lbMember.getMAC());
            assertTrue("protocol expected " + valuesArray[3][lbMember.getIndex()] + " found " +
                    lbMember.getProtocol(), valuesArray[3][lbMember.getIndex()] == lbMember.getProtocol());
            assertTrue("port expected " + valuesArray[4][lbMember.getIndex()] + " found " +
                    lbMember.getPort(), valuesArray[4][lbMember.getIndex()].equals(lbMember.getPort()));
        }
    }

    @Test
    public void testGetMembers() throws Exception {
        Map<String, LoadBalancerConfiguration.LoadBalancerPoolMember> lbMapLocal = lbConfig.getMembers();
        for (LoadBalancerConfiguration.LoadBalancerPoolMember lbMember: lbMapLocal.values()){
            assertTrue("IP expected " + valuesArray[1][lbMember.getIndex()] + " found: " +
                    lbMember.getIP(), valuesArray[1][lbMember.getIndex()] == lbMember.getIP());
            assertTrue("MAC expected " + valuesArray[2][lbMember.getIndex()] + " found " +
                    lbMember.getMAC(), valuesArray[2][lbMember.getIndex()] == lbMember.getMAC());
            assertTrue("protocol expected " + valuesArray[3][lbMember.getIndex()] + " found " +
                    lbMember.getProtocol(), valuesArray[3][lbMember.getIndex()] == lbMember.getProtocol());
            assertTrue("port expected " + valuesArray[4][lbMember.getIndex()] + " found " +
                    lbMember.getPort(), valuesArray[4][lbMember.getIndex()].equals(lbMember.getPort()));
        }
        assertEquals("Get members does not return members", lbMap, lbConfig.getMembers());
    }

    @Test
    public void testAddMember() throws Exception {
        lbConfig.addMember(valuesArray[0][2].toString(), valuesArray[1][2].toString(), valuesArray[2][2].toString(),
                valuesArray[3][2].toString(), (Integer)valuesArray[4][2]);
        Map<String, LoadBalancerConfiguration.LoadBalancerPoolMember> lbMapLocal2 = lbConfig.getMembers();
        assertTrue("Error, maps are unequal after adding a member. Size should be " + 3 +
                " but size is " + lbMapLocal2.size(), 3 == lbMapLocal2.size());
    }

    @Test
    public void testRemoveMember() throws Exception {
        lbConfig.addMember(valuesArray[0][2].toString(), valuesArray[1][2].toString(), valuesArray[2][2].toString(),
                valuesArray[3][2].toString(), (Integer)valuesArray[4][2]);
        Map<String, LoadBalancerConfiguration.LoadBalancerPoolMember> lbMapLocal = lbConfig.getMembers();
        assertTrue("Error, maps are not equal lbMapLocal size is " + lbMapLocal.size() + " size should be " +
                3, lbMapLocal.size() == 3);
        lbConfig.removeMember("mockUUID3");
        Map<String, LoadBalancerConfiguration.LoadBalancerPoolMember> lbMapLocal2 = lbConfig.getMembers();
        assertTrue("Error, maps are unequal after removing a member, size should be " + 2 + " but is " +
                lbMapLocal2.size(), 2 == lbMapLocal2.size());
    }

    @Test
    public void testIsValid() throws Exception {
        assertFalse("load balancer config returned true for isValid, should be true ", lbConfig.isValid());
        lbConfig.setProviderNetworkType("mockProviderNetworkType");
        assertTrue("load balancer config returned false for isValid, should be true ", lbConfig.isValid());
        lbConfig.removeMember("mockUUID2");
        lbConfig.removeMember("mockUUID1");
        assertFalse("load balancer config returned true for isValid, should be false ", lbConfig.isValid());
    }
}