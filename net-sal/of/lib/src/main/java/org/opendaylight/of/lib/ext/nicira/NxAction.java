/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.ext.nicira;

import org.opendaylight.of.lib.ExperimenterId;
import org.opendaylight.of.lib.ProtocolVersion;
import org.opendaylight.of.lib.instr.ActExperimenter;
import org.opendaylight.of.lib.instr.Action;
import org.opendaylight.of.lib.instr.ActionType;

/**
 * Abstract base class for Nicira extension actions.
 *
 * @author Simon Hunt
 */
// TODO: consider moving this to some other package -- [for now PoC]

abstract class NxAction extends ActExperimenter {

    NiciraActionSubtype nxActionSubtype;

    /**
     * Constructs a Nicira Extension action.
     *
     * @param nxSubtype the nicira extension subtype
     */
    NxAction(NiciraActionSubtype nxSubtype) {
        // FIXME: hard-coded protocol version 1.3 -- not the best solution
        super(ProtocolVersion.V_1_3, createHeader());

        // set our experimenter ID and the action subtype
        id = ExperimenterId.NICIRA.encodedId();
        nxActionSubtype = nxSubtype;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        final int slen = sb.length();
        final int dataEqNull = sb.indexOf("data=null");
        sb.replace(dataEqNull, slen, "");
        sb.append("nxAct=").append(nxActionSubtype);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns the Nicira Extension subtype for this action.
     *
     * @return the NX subtype
     */
    public NiciraActionSubtype getNxaSubtype() {
        return nxActionSubtype;
    }


    // deal with the header..
    protected void setHeaderLength(int len) {
        ((Header)header).setLength(len);
    }

    static Header createHeader() {
        return new Header();
    }

    static class Header extends Action.Header {
        Header() {
            type = ActionType.EXPERIMENTER;
            length = NiciraActionFactory.ACT_HEADER_LEN;
        }

        protected void setLength(int len) {
            length = len;
        }
    }
}
