/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Hugo Trippaers
 */
package org.opendaylight.ovsdb.neutron.provider;

import com.google.gson.internal.Pair;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.plugin.ConfigurationService;
import org.opendaylight.ovsdb.plugin.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OVSDBTestOF13ProviderBaseIT extends OvsdbOF13ProviderTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OVSDBTestOF13ProviderBaseIT.class);

    @Test
    public void getBridgeDomains() throws Throwable{

        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

        /**
         * List a Bridge Domain
         *
         * @param node Node serving this configuration service
         *
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        List<String> ls = configurationService.getBridgeDomains(node);
        System.out.println(ls);
    }
}
