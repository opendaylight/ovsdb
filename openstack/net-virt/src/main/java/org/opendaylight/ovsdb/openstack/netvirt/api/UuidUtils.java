/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Utilities for validating and converting OpenStack UUID's
 */
public final class UuidUtils {

    /**
     * Neutron UUID identifier length.
     */
    private static final int UUID_LEN = 36;

    /**
     * Tenant id length when keystone identifier is used in neutron.
     */
    private static final int KEYSTONE_ID_LEN = 32;

    /**
     * UUID version number position.
     */
    private static final int UUID_VERSION_POS = 12;

    /**
     * UUID time-low field byte length in hex.
     */
    private static final int UUID_TIME_LOW = 8;

    /**
     * UUID time-mid field byte length in hex.
     */
    private static final int UUID_TIME_MID = 4;

    /**
     * UUID time-high and version field byte length in hex.
     */
    private static final int UUID_TIME_HIGH_VERSION = 4;

    /**
     * UUID clock sequence field byte length in hex.
     */
    private static final int UUID_CLOCK_SEQ = 4;

    /**
     * UUID node field byte length in hex.
     */
    private static final int UUID_NODE = 12;

    /**
     * UUID time field byte length in hex.
     */
    private static final int UUID_TIME_LEN = (UUID_TIME_LOW +
                                              UUID_TIME_MID + UUID_TIME_HIGH_VERSION);

    private static final Logger LOG = LoggerFactory.getLogger(UuidUtils.class);

    /**
     * Private constructor (utility class).
     */
    private UuidUtils() {
        // Nothing to do
    }

    /**
     * Convert neutron object id to  key syntax.
     *
     * @param neutronID neutron object id.
     * @return key in compliance to  object key.
     */
    public static String convertNeutronIDToKey(String neutronID) {
        String key;
        if (neutronID == null) {
            return null;
        }

        LOG.trace(" neutronID - {}, length - {} ",
                     neutronID, neutronID.length());
        if (!isValidNeutronID(neutronID)) {
            return null;
        }

        if (neutronID.length() == UUID_LEN) {
            key = convertUUIDToKey(neutronID);
        } else if (neutronID.length() == KEYSTONE_ID_LEN) {
            key = convertKeystoneIDToKey(neutronID);
        } else {
            key = neutronID;
        }
        return key;
    }

    /**
     * Verify the validity of neutron object identifiers.
     *
     * @param id neutron object id.
     * @return {@code true} neutron identifier is valid.
     *         {@code false} neutron identifier is invalid.
     */
    public static boolean isValidNeutronID(String id) {
        if (id == null) {
            return false;
        }
        boolean isValid;
        LOG.trace("id - {}, length - {} ", id, id.length());
        /**
         * check the string length
         * if length is 36 its a uuid do uuid validation
         * if length is 32 it can be tenant id form keystone
         * if its less than 32  can be valid  ID
         */
        if (id.length() == UUID_LEN) {
            try {
                UUID fromUUID = UUID.fromString(id);
                String toUUID = fromUUID.toString();
                isValid = toUUID.equalsIgnoreCase(id);
            } catch(IllegalArgumentException e) {
                LOG.error(" IllegalArgumentExecption for id - {} ", id, e);
                isValid = false;
            }
        } else {
            isValid = (id.length() > 0) && (id.length() <= KEYSTONE_ID_LEN);
        }
        return isValid;
    }

    /**
     * Convert UUID to  key syntax.
     *
     * @param id neutron object UUID.
     * @return key in compliance to  object key.
     */
    private static String convertUUIDToKey(String id) {

        String key;
        if (id == null) {
            return null;
        }
        LOG.trace("id - {}, length - {} ", id, id.length());
        /**
         * ID must be less than 32 bytes,
         * Shorten UUID string length from 36 to 31 as follows:
         * delete UUID Version and hyphen (see RFC4122) field in the UUID
         */
        try {
            StringBuilder tKey = new StringBuilder();
            // remove all the '-'
            for (String retkey: id.split("-")) {
                tKey.append(retkey);
            }
            // remove the version byte
            tKey.deleteCharAt(UUID_VERSION_POS);
            key = tKey.toString();
        } catch(IllegalArgumentException ile) {
            LOG.error(" Invalid UUID - {} ", id, ile);
            key = null;
        }
        return key;
    }

    /**
     * Convert string id to  key syntax.
     *
     * @param id neutron object id.
     * @return key in compliance to  object key.
     */
    private static String convertKeystoneIDToKey(String id) {
        String key = null;
        if (id == null) {
            return null;
        }

        /**
         * tenant ID if given from openstack keystone does not follow the
         * generic UUID syntax, convert the ID to UUID format for validation
         * and reconvert it to  key
         */

        LOG.trace(" id - {}, length - {} ", id, id.length());
        try {
            StringBuilder tKey = new StringBuilder();
            String tmpStr = id.substring(0, UUID_TIME_LOW);
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring(UUID_TIME_LOW,
                                  (UUID_TIME_LOW + UUID_TIME_MID));
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring((UUID_TIME_LOW + UUID_TIME_MID),
                                  UUID_TIME_LEN);
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring(UUID_TIME_LEN,
                                  (UUID_TIME_LEN + UUID_CLOCK_SEQ));
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring((UUID_TIME_LEN + UUID_CLOCK_SEQ),
                                  (UUID_TIME_LEN + UUID_CLOCK_SEQ + UUID_NODE));
            tKey.append(tmpStr);

            tmpStr = tKey.toString();
            UUID fromUUID = UUID.fromString(tmpStr);
            String toUUID = fromUUID.toString();
            if (toUUID.equalsIgnoreCase(tmpStr)) {
                key = convertUUIDToKey(tmpStr);
            }
        } catch(IndexOutOfBoundsException ibe) {
            LOG.error(" Exception! Invalid UUID - {} ", id, ibe);
            key = null;
        } catch (IllegalArgumentException iae) {
            LOG.error(" Exception! Invalid object ID - {} ", id, iae);
            key = null;
        }
        return key;
    }

}
