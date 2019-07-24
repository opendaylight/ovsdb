/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to let Blueprint XML configure {@link HwvtepSouthboundProvider}.
 *
 * @author Chandra Shekar S
 */
public class HwvtepSouthboundProviderConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundProviderConfigurator.class);

    private static final String SHARD_STATUS_CHECK_RETRY_COUNT = "shard-status-check-retry-count";

    private final HwvtepSouthboundProvider hwvtepSouthboundProvider;

    public HwvtepSouthboundProviderConfigurator(HwvtepSouthboundProvider hwvtepSouthboundProvider) {
        this.hwvtepSouthboundProvider = hwvtepSouthboundProvider;
    }

    public void setShardStatusCheckRetryCount(int retryCount) {
        hwvtepSouthboundProvider.setShardStatusCheckRetryCount(retryCount);
    }



    public void updateConfigParameter(Map<String, Object> configParameters) {
        if (configParameters != null && !configParameters.isEmpty()) {
            LOG.debug("Config parameters received : {}", configParameters.entrySet());
            for (Map.Entry<String, Object> paramEntry : configParameters.entrySet()) {
                if (paramEntry.getKey().equalsIgnoreCase(SHARD_STATUS_CHECK_RETRY_COUNT)) {
                    hwvtepSouthboundProvider
                            .setShardStatusCheckRetryCount(Integer.parseInt((String) paramEntry.getValue()));
                }
            }
        }
    }
}
