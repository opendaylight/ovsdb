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
public class NeutronPort_VIFDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement (name = "port_filter")
    Boolean portFilter;

    @XmlElement (name = "ovs_hybrid_plug")
    Boolean ovsHybridPlug;

    public NeutronPort_VIFDetail() {
    }

    public NeutronPort_VIFDetail(Boolean portFilter, Boolean ovsHybridPlug) {
        this.portFilter = portFilter;
        this.ovsHybridPlug = ovsHybridPlug;
    }

    public Boolean getPortFilter() { return(portFilter); }

    public void setPortFilter(Boolean portFilter) { this.portFilter = portFilter; }

    public Boolean getOvsHybridPlug() { return(ovsHybridPlug); }

    public void setOvsHybridPlug(Boolean ovsHybridPlug) { this.ovsHybridPlug = ovsHybridPlug; }
}
