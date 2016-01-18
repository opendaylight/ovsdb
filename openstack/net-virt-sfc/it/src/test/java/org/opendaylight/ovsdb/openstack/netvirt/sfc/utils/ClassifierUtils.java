/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.utils;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.ClassifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.SffsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.SffBuilder;

public class ClassifierUtils extends AbstractUtils {
    public SffBuilder sffBuilder(SffBuilder sffBuilder, String sffName) {
        return sffBuilder.setName(sffName);
    }

    public SffsBuilder sffsBuilder(SffsBuilder sffsBuilder, SffBuilder sffBuilder) {
        List<Sff> sffList = new ArrayList<>();
        sffList.add(sffBuilder.build());
        sffsBuilder.setSff(sffList);

        return sffsBuilder;
    }

    public ClassifierBuilder classifierBuilder(ClassifierBuilder classifierBuilder,
                                               String classifierName, String aclName,
                                               SffsBuilder sffsBuilder) {
        return classifierBuilder
                .setName(classifierName)
                .setAcl(aclName);
    }

    public ClassifiersBuilder classifiersBuilder(ClassifiersBuilder classifiersBuilder,
                                                 ClassifierBuilder classifierBuilder) {
        List<Classifier> classifierList = new ArrayList<>();
        classifierList.add(classifierBuilder.build());
        classifiersBuilder.setClassifier(classifierList);

        return classifiersBuilder;
    }
}
