/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSecurityGroupAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSecurityRuleAware;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for OpenStack Neutron v2.0 Port Security API calls.
 */
public class PortSecurityHandler extends AbstractHandler
        implements INeutronSecurityGroupAware, INeutronSecurityRuleAware, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(PortSecurityHandler.class);
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile SecurityServicesManager securityServicesManager;

    @Override
    public int canCreateNeutronSecurityGroup(NeutronSecurityGroup neutronSecurityGroup) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSecurityGroupCreated(NeutronSecurityGroup neutronSecurityGroup) {
        int result = canCreateNeutronSecurityGroup(neutronSecurityGroup);
        if (result != HttpURLConnection.HTTP_CREATED) {
            LOG.debug("Neutron Security Group creation failed {} ", result);
        }
    }

    @Override
    public int canUpdateNeutronSecurityGroup(NeutronSecurityGroup delta, NeutronSecurityGroup original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityGroupUpdated(NeutronSecurityGroup neutronSecurityGroup) {
        // Nothing to do
    }

    @Override
    public int canDeleteNeutronSecurityGroup(NeutronSecurityGroup neutronSecurityGroup) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityGroupDeleted(NeutronSecurityGroup neutronSecurityGroup) {
        //TODO: Trigger flowmod removals
        int result = canDeleteNeutronSecurityGroup(neutronSecurityGroup);
        if  (result != HttpURLConnection.HTTP_OK) {
            LOG.error(" delete Neutron Security Rule validation failed for result - {} ", result);
        }
    }

    /**
     * Invoked when a Security Rules creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronSecurityRule  An instance of proposed new Neutron Security Rule object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronSecurityRule(NeutronSecurityRule neutronSecurityRule) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSecurityRuleCreated(NeutronSecurityRule neutronSecurityRule) {
        enqueueEvent(new NorthboundEvent(neutronSecurityRule, Action.ADD));
    }

    @Override
    public int canUpdateNeutronSecurityRule(NeutronSecurityRule delta, NeutronSecurityRule original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityRuleUpdated(NeutronSecurityRule neutronSecurityRule) {
        // Nothing to do
    }

    @Override
    public int canDeleteNeutronSecurityRule(NeutronSecurityRule neutronSecurityRule) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityRuleDeleted(NeutronSecurityRule neutronSecurityRule) {
        enqueueEvent(new NorthboundEvent(neutronSecurityRule, Action.DELETE));
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NorthboundEvent)) {
            LOG.error("Unable to process abstract event {}", abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                processNeutronSecurityRuleAdded(ev.getNeutronSecurityRule());
                break;
            case DELETE:
                processNeutronSecurityRuleDeleted(ev.getNeutronSecurityRule());
                break;
            default:
                LOG.warn("Unable to process event action {}", ev.getAction());
                break;
        }
    }

    private void processNeutronSecurityRuleAdded(NeutronSecurityRule neutronSecurityRule) {
        List<NeutronPort> portList = getPortWithSecurityGroup(neutronSecurityRule.getSecurityRuleGroupID());
        for (NeutronPort port:portList) {
            syncSecurityGroup(neutronSecurityRule,port,true);
        }
    }

    private void processNeutronSecurityRuleDeleted(NeutronSecurityRule neutronSecurityRule) {
        List<NeutronPort> portList = getPortWithSecurityGroup(neutronSecurityRule.getSecurityRuleGroupID());
        for (NeutronPort port:portList) {
            syncSecurityGroup(neutronSecurityRule,port,false);
        }
    }

    private void syncSecurityGroup(NeutronSecurityRule  securityRule,NeutronPort port,
                                   boolean write) {

        if (null != securityRule.getSecurityRemoteGroupID()) {
            List<Neutron_IPs> vmIpList  = securityServicesManager
                    .getVmListForSecurityGroup(port.getID(), securityRule.getSecurityRemoteGroupID());
            for (Neutron_IPs vmIp :vmIpList ) {
                securityServicesManager.syncSecurityRule(port, securityRule, vmIp, write);
            }
        } else {
            securityServicesManager.syncSecurityRule(port, securityRule, null, write);
        }
    }

    private List<NeutronPort> getPortWithSecurityGroup(String securityGroupUuid) {

        List<NeutronPort> neutronPortList = neutronPortCache.getAllPorts();
        List<NeutronPort> neutronPortInSG = new ArrayList<NeutronPort>();
        for (NeutronPort neutronPort:neutronPortList) {
            List<NeutronSecurityGroup> securityGroupList = neutronPort.getSecurityGroups();
            for (NeutronSecurityGroup neutronSecurityGroup:securityGroupList) {
                if (neutronSecurityGroup.getID().equals(securityGroupUuid)) {
                    neutronPortInSG.add(neutronPort);
                    break;
                }
            }
        }
        return neutronPortInSG;
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(serviceReference, this);
        neutronPortCache =
                (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
    }

    @Override
    public void setDependencies(Object impl) {}
}