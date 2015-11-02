/*
 * Copyright © 2015 Dell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.NetvirtSfcOF13Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcProvider.class);
    private NetvirtSfcAclListener aclListener;
    private NetvirtSfcClassifierListener classfierListener;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NetvirtSfcProvider Session Initiated");
        DataBroker dataBroker = session.getSALService(DataBroker.class);

        INetvirtSfcOF13Provider provider = new NetvirtSfcOF13Provider(dataBroker);
        aclListener = new NetvirtSfcAclListener(provider, dataBroker);
        classfierListener = new NetvirtSfcClassifierListener(provider, dataBroker);
    }

    @Override
    public void close() throws Exception {
        LOG.info("NetvirtSfcProvider Closed");
        aclListener.close();
        classfierListener.close();
    }
}
