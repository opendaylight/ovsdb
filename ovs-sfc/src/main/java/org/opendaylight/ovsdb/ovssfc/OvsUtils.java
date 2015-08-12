/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OvsUtils {
    private static final Logger logger = LoggerFactory.getLogger(OvsUtils.class);
    private OvsSfcProvider ovsSfcProvider = OvsSfcProvider.getOvsSfcProvider();

    private OvsdbConnectionService getOvsdbConnectionService () {
        return (OvsdbConnectionService) ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);
    }

    private OvsdbConfigurationService getOvsdbConfigurationService () {
        return (OvsdbConfigurationService) ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class, this);
    }

    public Node getNodeFromSystemId (String systemId) {
        Node node = null;

        OvsdbConnectionService connectionService = getOvsdbConnectionService();
        Preconditions.checkNotNull(connectionService);
        List<Node> ovsNodes = connectionService.getNodes();
        if (ovsNodes != null) {
            for (Node ovsNode : ovsNodes) {
                if (systemId.equals(getSystemId(ovsNode))) {
                    node = ovsNode;
                }
            }
        }

        logger.trace("\nOVSSFC {}\n ovsNode: {}",
                Thread.currentThread().getStackTrace()[1],
                node != null ? node.toString() : "");

        return node;
    }


    public String getSystemId (Node ovsNode) {
        String systemId = "";

        if (ovsNode == null) {
            return systemId;
        }
        OvsdbConfigurationService ovsdbConfigurationService = getOvsdbConfigurationService();
        if (ovsdbConfigurationService == null) {
            return systemId;
        }

        Map<String, Row> table =
                ovsdbConfigurationService.getRows(ovsNode, ovsdbConfigurationService.getTableName(ovsNode, OpenVSwitch.class));

        if (table == null) {
            logger.error("OpenVSwitch table is null for Node {} ", ovsNode);
            return systemId;
        }

        // Loop through all the Open_vSwitch rows looking for the first occurrence of external_ids.
        // The specification does not restrict the number of rows so we choose the first we find.
        for (Row row : table.values()) {
            OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(ovsNode, OpenVSwitch.class, row);
            Map<String, String> externalIds = ovsRow.getExternalIdsColumn().getData();
            if (externalIds == null) {
                continue;
            }

            systemId = externalIds.get("system-id");
        }

        logger.trace("\nOVSSFC {}\n system-id: {}",
                Thread.currentThread().getStackTrace()[1],
                systemId);

        return systemId;
    }

    public String getBridgeUUID (Node ovsNode, String bridgeName) {
        String uuid = "";

        if (ovsNode == null) {
            return uuid;
        }

        OvsdbConfigurationService ovsdbConfigurationService = getOvsdbConfigurationService();
        if (ovsdbConfigurationService == null) {
            return uuid;
        }

        try {
            Map<String, Row> table = ovsdbConfigurationService.
                    getRows(ovsNode, ovsdbConfigurationService.getTableName(ovsNode, Bridge.class));

            if (table != null) {
                for (String key : table.keySet()) {
                    Bridge bridge = ovsdbConfigurationService.getTypedRow(ovsNode, Bridge.class, table.get(key));
                    if (bridge.getName().equals(bridgeName)) {
                        uuid = key;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", ovsNode, bridgeName, e);
        }

        logger.trace("\nOVSSFC {}\n uuid: {}",
                Thread.currentThread().getStackTrace()[1],
                uuid);

        return uuid;
    }

    public Long getDpid (Node ovsNode, String bridgeName) {
        Long dpid = 0L;

        if (ovsNode == null) {
            return dpid;
        }

        logger.trace("\nOVSSFC Enter {}\n ovsNode: {}, bridgeName: {}",
                Thread.currentThread().getStackTrace()[1],
                ovsNode, bridgeName);

        String bridgeUuid = ovsSfcProvider.ovsUtils.getBridgeUUID(ovsNode, bridgeName);
        if (bridgeUuid == null) {
            return dpid;
        }

        logger.trace("\nOVSSFC {}\n ovsNode: {}, bridgeName: {}, uuid: {}",
                Thread.currentThread().getStackTrace()[1],
                ovsNode, bridgeName, bridgeUuid);

        OvsdbConfigurationService ovsdbConfigurationService = getOvsdbConfigurationService();
        try {
            Row bridgeRow = ovsdbConfigurationService
                    .getRow(ovsNode, ovsdbConfigurationService.getTableName(ovsNode, Bridge.class), bridgeUuid);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(ovsNode, Bridge.class, bridgeRow);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids != null && !dpids.isEmpty()) {
                dpid = HexEncode.stringToLong((String) dpids.toArray()[0]);
            }
        } catch (Exception e) {
            logger.error("Error finding Bridge's OF DPID", e);
        }
        return dpid;
    }
}
