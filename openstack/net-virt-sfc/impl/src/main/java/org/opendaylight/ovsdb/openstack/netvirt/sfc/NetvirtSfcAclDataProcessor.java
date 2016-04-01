/*
 * Copyright © 2015, 2016 Dell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Data processor for AccessList.
 */
public class NetvirtSfcAclDataProcessor implements INetvirtSfcDataProcessor<Acl> {
    private final INetvirtSfcOF13Provider provider;

    /**
     * {@link NetvirtSfcAclDataProcessor} constructor.
     * @param provider OpenFlow 1.3 Provider
     */
    public NetvirtSfcAclDataProcessor(final INetvirtSfcOF13Provider provider) {
        this.provider = Preconditions.checkNotNull(provider, "Provider can not be null!");
    }

    @Override
    public void remove(final InstanceIdentifier<Acl> identifier,
                       final Acl change) {
        Preconditions.checkNotNull(change, "Removed object can not be null!");
        provider.removeClassifierRules(change);
    }

    @Override
    public void update(final InstanceIdentifier<Acl> identifier,
                       final Acl original, final Acl change) {
        Preconditions.checkNotNull(original, "Updated original object can not be null!");
        Preconditions.checkNotNull(original, "Updated update object can not be null!");
        remove(identifier, original);
        provider.addClassifierRules(change);
    }

    @Override
    public void add(final InstanceIdentifier<Acl> identifier,
                    final Acl change) {
        Preconditions.checkNotNull(change, "Added object can not be null!");
        provider.addClassifierRules(change);
    }
}
