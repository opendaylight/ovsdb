/*
 * Copyright (c) 2016 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.utils;

public class RoutemgrUtil {
    public static final int ICMPv6_TYPE = 58;
    public static final short ICMPv6_RS_CODE = 133;
    public static final short ICMPv6_RA_CODE = 134;
    public static final short ICMPv6_NS_CODE = 135;
    public static final short ICMPv6_NA_CODE = 136;
    public static final short ICMPv6_MAX_HOP_LIMIT = 255;

    public static byte[] bytesFromHexString(String values) {
        String target = "";
        if(values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for(int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    public static String bytesToHexString(byte[] bytes) {
        if(bytes == null) {
            return "null";
        }
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < bytes.length; i++) {
            if(i > 0) {
                buf.append(":");
            }
            short u8byte = (short) (bytes[i] & 0xff);
            String tmp = Integer.toHexString(u8byte);
            if(tmp.length() == 1) {
                buf.append("0");
            }
            buf.append(tmp);
        }
        return (buf.toString());
    }
}
