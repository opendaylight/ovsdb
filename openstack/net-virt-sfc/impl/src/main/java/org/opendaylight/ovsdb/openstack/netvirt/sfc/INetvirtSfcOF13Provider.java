/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.Bridges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.osgi.framework.ServiceReference;

/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for Netvirt SFC
 * @author Arun Yerra
 */
public interface INetvirtSfcOF13Provider {

    /**
     * Method installs the OF rules corresponding to rules within ACL
     * on a given Service Function Forwarder. DataObject which is identified by InstanceIdentifier.
     *
     * @param bridge - Service Function Forwarder
     * @param acl - Access list includes rules that need to be installed in a SFF.
     */
    void addClassifierRules(Bridge bridge, Acl acl);
    void addClassifierRules(Bridges bridges, Acl acl);

    /**
     * Method removes the OF rules corresponding to rules within ACL
     * on a given Service Function Forwarder. DataObject which is identified by InstanceIdentifier.
     *
     * @param sff - Service Function Forwarder
     * @param acl - Access list includes rules that need to be installed in a SFF.
     */
    void removeClassifierRules(Sff sff, Acl acl);

    void addClassifierRules(Acl acl);
    void removeClassifierRules(Acl acl);

    void setSfcClassifierService(ISfcClassifierService sfcClassifierService);
    public void setDependencies(ServiceReference serviceReference);
}
