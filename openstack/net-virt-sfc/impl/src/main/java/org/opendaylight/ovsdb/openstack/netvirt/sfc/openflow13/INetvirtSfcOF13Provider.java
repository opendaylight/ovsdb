/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;

/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for Netvirt SFC
 * @author Arun Yerra
 */
public interface INetvirtSfcOF13Provider {

    /**
     * Method installs the OF rules corresponding to rules within ACL
     * on a given Service Function Forwarder. DataObject which is identified by InstanceIdentifier.
     *
     * @param sff - Service Function Forwarder
     * @param acl - Access list includes rules that need to be installed in a SFF.
     */
    public void addClassifierRules(Sff sff, AccessList acl);

    /**
     * Method removes the OF rules corresponding to rules within ACL
     * on a given Service Function Forwarder. DataObject which is identified by InstanceIdentifier.
     *
     * @param sff - Service Function Forwarder
     * @param acl - Access list includes rules that need to be installed in a SFF.
     */
    public void removeClassifierRules(Sff sff, AccessList acl);
}
