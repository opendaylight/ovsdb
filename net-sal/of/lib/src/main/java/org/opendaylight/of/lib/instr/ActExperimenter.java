/*
 * (c) Copyright 2012-2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.of.lib.instr;

import org.opendaylight.of.lib.ExperimenterId;
import org.opendaylight.of.lib.ProtocolVersion;
import org.opendaylight.util.ByteUtils;

/**
 * Flow action {@code EXPERIMENTER}.
 *
 * @author Simon Hunt
 */
public class ActExperimenter extends Action {
    /** Experimenter id (encoded). */
    protected int id;

    /** Experimenter defined data. */
    protected byte[] data;

    /**
     * Constructs an experimenter action.
     *
     * @param pv the protocol version
     * @param header the action header
     */
    protected ActExperimenter(ProtocolVersion pv, Header header) {
        super(pv, header);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        int len = sb.length();
        sb.replace(len-1, len, ",expId=")
                .append(ExperimenterId.idToString(id));
        if (data == null)
            sb.append(",data=null");
        else
            sb.append(",data=0x").append(ByteUtils.hex(data));
        sb.append("}");
        return sb.toString();
    }

    /** Returns the experimenter ID encoded as an int.
     *
     * @return the experimenter ID (encoded)
     */
    public int getId() {
        return id;
    }

    /** Returns the experimenter ID (if we know it); null otherwise.
     *
     * @return the experimenter ID
     */
    public ExperimenterId getExpId() {
        return ExperimenterId.decode(id);
    }

    /** Returns a copy of the experimenter-defined data.
     *
     * @return a copy of the data
     */
    public byte[] getData() {
        return data == null ? null : data.clone();
    }

}
