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
            LOG.warn("Netvirt RenderedServicePath DataChange listener registration failed!", e);
            throw new IllegalStateException("RspListener startup failed! System needs restart.", e);
        }
    }

    @Override
    public void remove(final InstanceIdentifier<RenderedServicePath> identifier, final RenderedServicePath change) {
        Preconditions.checkNotNull(change, "Removed object can not be null!");
        provider.removeRsp(change);
    }

    @Override
    public void update(final InstanceIdentifier<RenderedServicePath> identifier, final RenderedServicePath original,
                       RenderedServicePath change) {
        Preconditions.checkNotNull(original, "Updated original object can not be null!");
        Preconditions.checkNotNull(original, "Updated update object can not be null!");
        remove(identifier, original);
        provider.addRsp(change);
    }

    @Override
    public void add(final InstanceIdentifier<RenderedServicePath> identifier, final RenderedServicePath change) {
        Preconditions.checkNotNull(change, "Created object can not be null!");
        provider.addRsp(change);
    }

    @Override
    public void close() throws Exception {

    }
}
