/*
 * Copyright © 2015, 2016 Dell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Data tree listener for Classifier.
 *
 * @author Arun Yerra
 */
public class NetvirtSfcClassifierListener extends DelegatingDataTreeListener<Classifier> {
    /**
     * {@link NetvirtSfcClassifierListener} constructor.
     * @param provider OpenFlow 1.3 Provider
     * @param db MdSal {@link DataBroker}
     */
    public NetvirtSfcClassifierListener(final INetvirtSfcOF13Provider provider, final DataBroker db) {
        super(provider, new NetvirtSfcClassifierDataProcessor(provider, db), db,
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(Classifiers.class).child(Classifier.class)));
    }
}
