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
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessListKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcAclListener extends AbstractDataTreeListener<AccessList> {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcAclListener.class);
    private ListenerRegistration<NetvirtSfcAclListener> listenerRegistration;
    private MdsalUtils dbutils;

    public NetvirtSfcAclListener (final INetvirtSfcOF13Provider provider, final DataBroker db) {
        super(provider, AccessList.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");

        dbutils = new MdsalUtils(db);
        registrationListener(db);
    }

    private void registrationListener(final DataBroker db) {
        final DataTreeIdentifier<AccessList> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getIetfAclIid());
        try {
            LOG.info("Registering Data Change Listener for Netvirt AccesList configuration.");
            listenerRegistration = db.registerDataTreeChangeListener(treeId, this);
        } catch (final Exception e) {
            LOG.warn("Netvirt AccesList DataChange listener registration fail!");
            LOG.debug("Netvirt AccesList DataChange listener registration fail!", e);
            throw new IllegalStateException("NetvirtSfcAccesListListener startup fail! System needs restart.", e);
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
    public void remove(final InstanceIdentifier<AccessList> identifier,
                       final AccessList removeDataObj) {
        Preconditions.checkNotNull(removeDataObj, "Removed object can not be null!");
        String aclName = removeDataObj.getAclName();

        Classifiers classifiers = dbutils.read(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        if(classifiers != null) {
            for(Classifier classifier : classifiers.getClassifier()) {
                if(classifier.getAcl().equalsIgnoreCase(aclName)) {
                    if(classifier.getSffs() != null) {
                        for(Sff sff : classifier.getSffs().getSff()) {
                            provider.removeClassifierRules(sff, removeDataObj);
                        }
                    }
                }
            }
        }
        return;
    }

    @Override
    public void update(final InstanceIdentifier<AccessList> identifier,
                       final AccessList original, final AccessList update) {
    }

    @Override
    public void add(final InstanceIdentifier<AccessList> identifier,
                    final AccessList addDataObj) {
        Preconditions.checkNotNull(addDataObj, "Added object can not be null!");
        String aclName = addDataObj.getAclName();
        LOG.debug("Adding accesslist = {}", identifier);
        Classifiers classifiers = dbutils.read(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        if(classifiers != null) {
            for(Classifier classifier : classifiers.getClassifier()) {
                if(classifier.getAcl().equalsIgnoreCase(aclName)) {
                    if(classifier.getSffs() != null) {
                        for(Sff sff : classifier.getSffs().getSff()) {
                            provider.addClassifierRules(sff, addDataObj);
                        }
                    }
                }
            }
        }
        return;
    }

    private InstanceIdentifier<Classifiers> getClassifierIid () {
        return InstanceIdentifier.create(Classifiers.class);
    }

    public InstanceIdentifier<AccessList> getIetfAclIid () {
        return InstanceIdentifier.create(AccessLists.class).child(AccessList.class);
    }

    public InstanceIdentifier<AccessListEntry> getIetfAclEntryIid (String aclName, String ruleName) {
        return InstanceIdentifier.create(AccessLists.class).child(AccessList.class, new AccessListKey(aclName)).
                child(AccessListEntries.class).child(AccessListEntry.class, new AccessListEntryKey(ruleName));
    }
}
