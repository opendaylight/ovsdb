/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;

/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for Netvirt SFC
 * @author Arun Yerra
 */
public class NetvirtSfcOF13Provider implements INetvirtSfcOF13Provider{

    private final DataBroker dataService;

    public NetvirtSfcOF13Provider(final DataBroker dataBroker) {
        this.dataService = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
    }

    @Override
    public void addClassifierRules(Sff sff, AccessList acl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeClassifierRules(Sff sff, AccessList acl) {
        // TODO Auto-generated method stub

    }
}
