/*
 * Copyright (c) 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Neutron_ID implements Serializable {
    private static final long serialVersionUID = 1L;

    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name = "id")
    String uuid;

    public Neutron_ID() { }

    public Neutron_ID(String uuid) {
        this.uuid = uuid;
    }

    public String getID() {
        return uuid;
    }

    public void setID(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "Neutron_ID{" + "id='" + uuid + '\'' + "}";
    }
}
