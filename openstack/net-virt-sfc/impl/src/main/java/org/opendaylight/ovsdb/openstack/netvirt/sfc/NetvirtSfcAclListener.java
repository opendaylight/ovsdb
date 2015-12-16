/*
 * Copyright © 2015 Dell, Inc. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data tree listener for AccessList.
 */
public class NetvirtSfcAclListener extends AbstractDataTreeListener<Acl> {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcAclListener.class);
    private ListenerRegistration<NetvirtSfcAclListener> listenerRegistration;

    /**
     * {@link NetvirtSfcAclListener} constructor.
     * @param provider OpenFlow 1.3 Provider
     * @param db MdSal {@link DataBroker}
     */
    public NetvirtSfcAclListener(final INetvirtSfcOF13Provider provider, final DataBroker db) {
        super(provider, Acl.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");

        registrationListener(db);
    }

    private void registrationListener(final DataBroker db) {
        final DataTreeIdentifier<Acl> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getIetfAclIid());
        try {
            LOG.info("Registering Data Change Listener for NetvirtSfc AccessList configuration.");
            listenerRegistration = db.registerDataTreeChangeListener(treeId, this);
        } catch (final Exception e) {
            LOG.warn("Netvirt AccesList DataChange listener registration fail!");
            throw new IllegalStateException("NetvirtSfcAccessListListener startup fail! System needs restart.", e);
        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error while stopping IETF ACL ChangeListener: {}", e.getMessage());
                LOG.debug("Error while stopping IETF ACL ChangeListener..", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<Acl> identifier,
                       final Acl removeDataObj) {
        Preconditions.checkNotNull(removeDataObj, "Removed object can not be null!");
        provider.removeClassifierRules(removeDataObj);
    }

    @Override
    public void update(final InstanceIdentifier<Acl> identifier,
                       final Acl original, final Acl update) {
    }

    @Override
    public void add(final InstanceIdentifier<Acl> identifier,
                    final Acl addDataObj) {
        Preconditions.checkNotNull(addDataObj, "Added object can not be null!");
        LOG.debug("Adding accesslist iid = {}, dataObj = {}", identifier, addDataObj);
        provider.addClassifierRules(addDataObj);
    }

    public InstanceIdentifier<Acl> getIetfAclIid() {
        return InstanceIdentifier.create(AccessLists.class).child(Acl.class);
    }
}
