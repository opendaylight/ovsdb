/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data tree listener for {@link RenderedServicePath}
 */
public class RspListener extends AbstractDataTreeListener<RenderedServicePath> {
    private static final Logger LOG = LoggerFactory.getLogger(RspListener.class);
    private ListenerRegistration<RspListener> listenerRegistration;

    public RspListener(final INetvirtSfcOF13Provider provider, final DataBroker db) {
        super(provider, RenderedServicePath.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");

        registrationListener(db);
    }

    public InstanceIdentifier<RenderedServicePath> getRspIid() {
        return InstanceIdentifier.create(RenderedServicePaths.class).child(RenderedServicePath.class);
    }

    private void registrationListener(final DataBroker db) {
        final DataTreeIdentifier<RenderedServicePath> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getRspIid());
        try {
            LOG.info("Registering Data Change Listener for NetvirtSfc RenderedServicePath configuration.");
            listenerRegistration = db.registerDataTreeChangeListener(treeId, this);
        } catch (final Exception e) {
            LOG.warn("Netvirt RenderedServicePath DataChange listener registration failed!");
            throw new IllegalStateException("RspListener startup failed! System needs restart.", e);
        }
    }

    @Override
    public void remove(InstanceIdentifier<RenderedServicePath> identifier, RenderedServicePath change) {
        Preconditions.checkNotNull(change, "Object can not be null!");
        LOG.debug("remove RenderedServicePath iid = {}, change = {}", identifier, change);
        provider.removeRsp(change);
    }

    @Override
    public void update(InstanceIdentifier<RenderedServicePath> identifier, RenderedServicePath original,
                       RenderedServicePath change) {
        Preconditions.checkNotNull(change, "Object can not be null!");
        LOG.debug("Update RenderedServicePath iid = {}, change = {}", identifier, change);
        //provider.addClassifierRules(update);
    }

    @Override
    public void add(InstanceIdentifier<RenderedServicePath> identifier, RenderedServicePath change) {
        Preconditions.checkNotNull(change, "Object can not be null!");
        LOG.debug("Add RenderedServicePath iid = {}, change = {}", identifier, change);
    }

    @Override
    public void close() throws Exception {

    }
}
