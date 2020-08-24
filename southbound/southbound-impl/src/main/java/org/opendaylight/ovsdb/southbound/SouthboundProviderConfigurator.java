/*
 * Copyright (c) 2019 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to let Blueprint XML configure {@link SouthboundProvider}.
 *
 * @author Michael Vorburger.ch
 */
public class SouthboundProviderConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProviderConfigurator.class);

    private static final String SKIP_MONITORING_MANAGER_STATUS_PARAM = "skip-monitoring-manager-status";
    private static final String BRIDGES_RECONCILIATION_INCLUSION_LIST_PARAM = "bridges-reconciliation-inclusion-list";
    private static final String BRIDGES_RECONCILIATION_EXCLUSION_LIST_PARAM = "bridges-reconciliation-exclusion-list";

    private final SouthboundProvider southboundProvider;

    public SouthboundProviderConfigurator(SouthboundProvider southboundProvider) {
        this.southboundProvider = southboundProvider;
    }

    public void setSkipMonitoringManagerStatus(boolean flag) {
        southboundProvider.setSkipMonitoringManagerStatus(flag);
    }

    public void setBridgesReconciliationInclusionList(String bridgeListStr) {
        if (bridgeListStr != null && !bridgeListStr.equals("")) {
            SouthboundProvider.setBridgesReconciliationInclusionList(getBridgesList(bridgeListStr));
        }
    }

    public void setBridgesReconciliationExclusionList(String bridgeListStr) {
        if (bridgeListStr != null && !bridgeListStr.equals("")) {
            SouthboundProvider.setBridgesReconciliationExclusionList(getBridgesList(bridgeListStr));
        }
    }

    private static List<String> getBridgesList(final String bridgeListStr) {
        List<String> bridgeList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(bridgeListStr, ",");
        while (tokenizer.hasMoreTokens()) {
            bridgeList.add(tokenizer.nextToken());
        }
        return bridgeList;

    }

    public void updateConfigParameter(Map<String, Object> configParameters) {
        if (configParameters != null && !configParameters.isEmpty()) {
            LOG.debug("Config parameters received : {}", configParameters.entrySet());
            for (Map.Entry<String, Object> paramEntry : configParameters.entrySet()) {
                if (paramEntry.getKey().equalsIgnoreCase(SKIP_MONITORING_MANAGER_STATUS_PARAM)) {
                    southboundProvider
                            .setSkipMonitoringManagerStatus(Boolean.parseBoolean((String) paramEntry.getValue()));
                } else if (paramEntry.getKey().equalsIgnoreCase(BRIDGES_RECONCILIATION_INCLUSION_LIST_PARAM)) {
                    String bridgeListStr = (String)paramEntry.getValue();
                    if (bridgeListStr != null && !bridgeListStr.equals("")) {
                        SouthboundProvider.setBridgesReconciliationInclusionList(getBridgesList(bridgeListStr));
                    }
                } else if (paramEntry.getKey().equalsIgnoreCase(BRIDGES_RECONCILIATION_EXCLUSION_LIST_PARAM)) {
                    String bridgeListStr = (String)paramEntry.getValue();
                    if (bridgeListStr != null && !bridgeListStr.equals("")) {
                        SouthboundProvider.setBridgesReconciliationExclusionList(getBridgesList(bridgeListStr));
                    }
                }
            }
        }
    }
}
