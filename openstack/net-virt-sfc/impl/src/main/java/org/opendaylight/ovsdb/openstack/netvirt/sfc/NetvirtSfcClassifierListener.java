/*
 * Copyright © 2015 Dell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.sfc;

/**
 * @author Arun Yerra
 *
 */

import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessListKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.Sffs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcClassifierListener extends AbstractDataTreeListener<Classifier> {

    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcClassifierListener.class);
    private MdsalUtils dbutils;
    private ListenerRegistration<NetvirtSfcClassifierListener> listenerRegistration;

    public NetvirtSfcClassifierListener (final INetvirtSfcOF13Provider provider, final DataBroker db) {
        super(provider, Classifier.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        dbutils = new MdsalUtils(db);
        registrationListener(db);
    }

    private void registrationListener(final DataBroker db) {
        final DataTreeIdentifier<Classifier> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        try {
            LOG.info("Registering Data Change Listener for Netvirt Classifier configuration.");
            listenerRegistration = db.registerDataTreeChangeListener(treeId, this);
        } catch (final Exception e) {
            LOG.warn("Netvirt Classifier DataChange listener registration fail!");
            LOG.debug("Netvirt Classifier DataChange listener registration fail!", e);
            throw new IllegalStateException("NetvirtSfcClassifierListener startup fail! System needs restart.", e);
        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error to stop Netvirt Classifier DataChange listener: {}", e.getMessage());
                LOG.debug("Error to stop Netvirt Classifier DataChange listener..", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<Classifier> identifier,
                       final Classifier removeDataObj) {
        Preconditions.checkNotNull(removeDataObj, "Added object can not be null!");
        String aclName = removeDataObj.getAcl();
        // Read the ACL information from data store and make sure it exists.
        AccessList acl = dbutils.read(LogicalDatastoreType.CONFIGURATION,getIetfAclIid(aclName));
        if(acl == null) {
            LOG.debug("IETF ACL with name ={} is not yet configured. skip this operation", aclName);
            return;
        }
        if(removeDataObj.getSffs() != null) {
            for(Sff sff : removeDataObj.getSffs().getSff()) {
                // Netvirt classifier binds an ACL with service function forwarder that is identified by SFF name.
                // SFF validation can be done with SFC Provider APIs, as SFF is configured within SFC project.  
                // Netvirt SFC provider will validate the SFF using SFC provider APIs.
                provider.removeClassifierRules(sff, acl);
            }
        }
    }

    @Override
    public void update(final InstanceIdentifier<Classifier> identifier,
                       final Classifier original, final Classifier update) {
        //TODO

    }

    @Override
    public void add(final InstanceIdentifier<Classifier> identifier,
                    final Classifier addDataObj) {
        Preconditions.checkNotNull(addDataObj, "Added object can not be null!");
        String aclName = addDataObj.getAcl();
        // Read the ACL information from data store and make sure it exists.
        AccessList acl = dbutils.read(LogicalDatastoreType.CONFIGURATION,getIetfAclIid(aclName));
        if(acl == null) {
            LOG.debug("IETF ACL with name ={} is not yet configured. skip this operation", aclName);
            return;
        }
        if(addDataObj.getSffs() != null) {
            for(Sff sff : addDataObj.getSffs().getSff()) {
                // Netvirt classifier binds an ACL with service function forwarder that is identified by SFF name.
                // SFF validation can be done with SFC Provider APIs, as SFF is configured within SFC project.  
                // Netvirt SFC provider will validate the SFF using SFC provider APIs.
                provider.addClassifierRules(sff, acl);
            }
        }
    }

    public InstanceIdentifier<Classifier> getClassifierIid () {
        return InstanceIdentifier.create(Classifiers.class).child(Classifier.class);
    }

    private InstanceIdentifier<AccessList> getIetfAclIid (String aclName) {
        return InstanceIdentifier.create(AccessLists.class).child(AccessList.class, new AccessListKey(aclName));
    }

}
