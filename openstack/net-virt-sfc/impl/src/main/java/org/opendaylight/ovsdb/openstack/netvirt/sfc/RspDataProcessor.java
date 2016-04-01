/*
 * Copyright © 2015, 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Data processor for {@link RenderedServicePath}
 */
public class RspDataProcessor implements INetvirtSfcDataProcessor<RenderedServicePath> {
    private final INetvirtSfcOF13Provider provider;

    public RspDataProcessor(final INetvirtSfcOF13Provider provider) {
        this.provider = Preconditions.checkNotNull(provider, "Provider can not be null!");
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
}
