/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron.impl;

import java.net.HttpURLConnection;
import java.util.UUID;

import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base utility functions used by neutron handlers.
 */
public class BaseHandler {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(BaseHandler.class);

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

    /**
     * Convert failure status returned by the  manager into
     * neutron API service errors.
     *
     * @param status  manager status
     * @return  An error to be returned to neutron API service.
     */
    protected static final int getException(Status status) {
        int result = HttpURLConnection.HTTP_INTERNAL_ERROR;

        assert !status.isSuccess();

        StatusCode code = status.getCode();
        logger.debug(" Execption code - {}, description - {}",
                code, status.getDescription());

        if (code == StatusCode.BADREQUEST) {
            result = HttpURLConnection.HTTP_BAD_REQUEST;
        } else if (code == StatusCode.CONFLICT) {
            result = HttpURLConnection.HTTP_CONFLICT;
        } else if (code == StatusCode.NOTACCEPTABLE) {
            result = HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        } else if (code == StatusCode.NOTFOUND) {
            result = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            result = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }

        return result;
    }

    /**
     * Verify the validity of neutron object identifiers.
     *
     * @param id neutron object id.
     * @return {@code true} neutron identifier is valid.
     *         {@code false} neutron identifier is invalid.
     */
    protected static final boolean isValidNeutronID(String id) {
        if (id == null) {
            return false;
        }
        boolean isValid = false;
        logger.trace("id - {}, length - {} ", id, id.length());
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
                logger.error(" IllegalArgumentExecption for id - {} ", id);
                isValid = false;
            }
        } else if ((id.length() > 0) && (id.length() <= KEYSTONE_ID_LEN)) {
            isValid = true;
        } else {
            isValid = false;
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

        String key = null;
        if (id == null) {
            return key;
        }
        logger.trace("id - {}, length - {} ", id, id.length());
        /**
         *  ID must be less than 32 bytes,
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
            logger.error(" Invalid UUID - {} ", id);
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
            return key;
        }

        /**
         * tenant ID if given from openstack keystone does not follow the
         * generic UUID syntax, convert the ID to UUID format for validation
         * and reconvert it to  key
         */

        logger.trace(" id - {}, length - {} ", id, id.length());
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
            logger.error(" Execption! Invalid UUID - {} ", id);
            key = null;
        } catch (IllegalArgumentException iae) {
            logger.error(" Execption! Invalid object ID - {} ", id);
            key = null;
        }
        return key;
    }

    /**
     * Convert neutron object id to  key syntax.
     *
     * @param neutronID neutron object id.
     * @return key in compliance to  object key.
     */
    protected static final String convertNeutronIDToKey(String neutronID) {
        String key = null;
        if (neutronID == null) {
            return key;
        }

        logger.trace(" neutronID - {}, length - {} ",
                neutronID, neutronID.length());
        if (!isValidNeutronID(neutronID)) {
            return key;
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

    protected IContainerManager containerManager;

    public IContainerManager getContainerManager() {
        return containerManager;
    }

    public void unsetContainerManager(IContainerManager s) {
        if (s == this.containerManager) {
            this.containerManager = null;
        }
    }

    public void setContainerManager(IContainerManager s) {
        this.containerManager = s;
    }

    protected IForwardingRulesManager frm;

    public IForwardingRulesManager getForwardingRulesManager() {
        return frm;
    }

    public void unsetForwardingRulesManager(IForwardingRulesManager s) {
        if (s == this.frm) {
            this.frm = null;
        }
    }

    public void setForwardingRulesManager(IForwardingRulesManager s) {
        this.frm = s;
    }

    protected OVSDBConfigService ovsdbConfigService;

    public OVSDBConfigService getOVSDBConfigService() {
        return ovsdbConfigService;
    }

    public void unsetOVSDBConfigService(OVSDBConfigService s) {
        if (s == this.ovsdbConfigService) {
            this.ovsdbConfigService = null;
        }
    }

    public void setOVSDBConfigService(OVSDBConfigService s) {
        this.ovsdbConfigService = s;
    }

    protected IConnectionServiceInternal connectionService;

    public IConnectionServiceInternal getConnectionService() {
        return connectionService;
    }

    public void unsetConnectionService(IConnectionServiceInternal s) {
        if (s == this.connectionService) {
            this.connectionService = null;
        }
    }

    public void setConnectionService(IConnectionServiceInternal s) {
        this.connectionService = s;
    }

    protected INeutronPortCRUD neutronPortCache;
    public INeutronPortCRUD getNeutronPortCRUD() {
        return neutronPortCache;
    }

    public void unsetNeutronPortCRUD(INeutronPortCRUD s) {
        if (s == this.neutronPortCache) {
            this.neutronPortCache = null;
        }
    }

    public void setNeutronPortCRUD(INeutronPortCRUD s) {
        this.neutronPortCache = s;
    }

    protected INeutronSubnetCRUD neutronSubnetCache;
    public INeutronSubnetCRUD getNeutronSubnetCRUD() {
        return neutronSubnetCache;
    }

    public void unsetNeutronSubnetCRUD(INeutronSubnetCRUD s) {
        if (s == this.neutronSubnetCache) {
            this.neutronSubnetCache = null;
        }
    }

    public void setNeutronSubnetCRUD(INeutronSubnetCRUD s) {
        this.neutronSubnetCache = s;
    }

    protected INeutronNetworkCRUD neutronNetworkCache;
    public INeutronNetworkCRUD getNeutronNetworkCRUD() {
        return neutronNetworkCache;
    }

    public void unsetNeutronNetworkCRUD(INeutronNetworkCRUD s) {
        if (s == this.neutronNetworkCache) {
            this.neutronNetworkCache = null;
        }
    }

    public void setNeutronNetworkCRUD(INeutronNetworkCRUD s) {
        this.neutronNetworkCache = s;
    }
}
