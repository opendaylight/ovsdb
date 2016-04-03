/*
 * Copyright © 2015, 2016 Dell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers
        .Classifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data processor for Classifier.
 */
public class NetvirtSfcClassifierDataProcessor implements INetvirtSfcDataProcessor<Classifier> {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcClassifierDataProcessor.class);
    private final MdsalUtils mdsalUtils;
    private final INetvirtSfcOF13Provider provider;

    /**
     * {@link NetvirtSfcClassifierDataProcessor} constructor.
     * @param provider OpenFlow 1.3 Provider
     * @param db MdSal {@link DataBroker}
     */
    public NetvirtSfcClassifierDataProcessor(final INetvirtSfcOF13Provider provider, final DataBroker db) {
        this.provider = Preconditions.checkNotNull(provider, "Provider can not be null!");
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        mdsalUtils = new MdsalUtils(db);
    }

    @Override
    public void remove(final InstanceIdentifier<Classifier> identifier,
                       final Classifier change) {
        Preconditions.checkNotNull(change, "Added object can not be null!");
        String aclName = change.getAcl();
        // Read the ACL information from data store and make sure it exists.
        Acl acl = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, getIetfAclIid(aclName));
        if (acl == null) {
            LOG.debug("IETF ACL with name ={} is not yet configured. skip this operation", aclName);
            return;
        }

        provider.removeClassifierRules(acl);
    }

    @Override
    public void update(final InstanceIdentifier<Classifier> identifier,
                       final Classifier original, final Classifier change) {
        //TODO

    }

    @Override
    public void add(final InstanceIdentifier<Classifier> identifier,
                    final Classifier change) {
        Preconditions.checkNotNull(change, "Added object can not be null!");
        String aclName = change.getAcl();
        // Read the ACL information from data store and make sure it exists.
        Acl acl = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, getIetfAclIid(aclName));
        if (acl == null) {
            LOG.debug("IETF ACL with name ={} is not yet configured. skip this operation", aclName);
            return;
        }

        provider.addClassifierRules(acl);
    }

    private InstanceIdentifier<Acl> getIetfAclIid(String aclName) {
        return InstanceIdentifier.create(AccessLists.class).child(Acl.class, new AclKey(aclName));
    }
}
