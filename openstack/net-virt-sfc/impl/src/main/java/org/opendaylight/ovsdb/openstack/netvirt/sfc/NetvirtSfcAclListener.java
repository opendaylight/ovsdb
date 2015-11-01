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
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.AceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
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
    private MdsalUtils dbutils;

    /**
     * {@link NetvirtSfcAclListener} constructor.
     * @param provider OpenFlow 1.3 Provider
     * @param db MdSal {@link DataBroker}
     */
    public NetvirtSfcAclListener(final INetvirtSfcOF13Provider provider, final DataBroker db) {
        super(provider, Acl.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");

        dbutils = new MdsalUtils(db);
        registrationListener(db);
    }

    private void registrationListener(final DataBroker db) {
        final DataTreeIdentifier<Acl> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getIetfAclIid());
        try {
            LOG.info("Registering Data Change Listener for NetvirtSfc AccesList configuration.");
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
    public void remove(final InstanceIdentifier<Acl> identifier,
                       final Acl removeDataObj) {
        Preconditions.checkNotNull(removeDataObj, "Removed object can not be null!");
        String aclName = removeDataObj.getAclName();

        Classifiers classifiers = dbutils.read(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        if (classifiers != null) {
            for (Classifier classifier : classifiers.getClassifier()) {
                if (classifier.getAcl().equalsIgnoreCase(aclName)) {
                    if (classifier.getSffs() != null) {
                        for (Sff sff : classifier.getSffs().getSff()) {
                            provider.removeClassifierRules(sff, removeDataObj);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void update(final InstanceIdentifier<Acl> identifier,
                       final Acl original, final Acl update) {
    }

    @Override
    public void add(final InstanceIdentifier<Acl> identifier,
                    final Acl addDataObj) {
        Preconditions.checkNotNull(addDataObj, "Added object can not be null!");
        String aclName = addDataObj.getAclName();
        LOG.debug("Adding accesslist iid = {}, dataObj = {}", identifier, addDataObj);
        Classifiers classifiers = dbutils.read(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        if (classifiers == null) {
            LOG.debug("add: No Classifiers found");
            return;
        }

        LOG.debug("add: Classifiers: {}", classifiers);
        for (Classifier classifier : classifiers.getClassifier()) {
            if (classifier.getAcl().equals(aclName)) {
                if (classifier.getBridges() != null) {
                    for (Bridge bridge : classifier.getBridges().getBridge()) {
                        provider.addClassifierRules(bridge, addDataObj);
                    }
                }
            }
        }
    }

    private InstanceIdentifier<Classifiers> getClassifierIid() {
        return InstanceIdentifier.create(Classifiers.class);
    }

    public InstanceIdentifier<Acl> getIetfAclIid() {
        return InstanceIdentifier.create(AccessLists.class).child(Acl.class);
    }

    /**
     * Create an {@link Ace} {@link InstanceIdentifier}.
     * @param aclName is the name of the ACL
     * @param ruleName is the name of the rule
     * @return the {@link Ace} {@link InstanceIdentifier}
     */
    public InstanceIdentifier<Ace> getIetfAclEntryIid(String aclName, String ruleName) {
        return InstanceIdentifier.create(AccessLists.class).child(Acl.class,
                new AclKey(aclName)).child(AccessListEntries.class).child(Ace.class,
                new AceKey(ruleName));
    }
}
