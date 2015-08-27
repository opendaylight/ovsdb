/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.util.Calendar;

import javax.inject.Inject;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ObjectArrays;

public abstract class AbstractMdsalTestBase extends AbstractConfigTestBase implements BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMdsalTestBase.class);
    private static final int REGISTRATION_TIMEOUT = 10000;
    @Inject @Filter(timeout = 60000)
    private BindingAwareBroker broker;
    private ProviderContext session = null;

    public ProviderContext getSession() {
        return session;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("Session Initiated: {}",session);
        this.session = session;
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        Calendar start = Calendar.getInstance();
        broker.registerProvider(this);
        for (int timer = 0;timer < REGISTRATION_TIMEOUT;timer++) {
            if (session != null) {
                Calendar stop = Calendar.getInstance();
                LOG.info("Registered with the MD-SAL after {} ms",
                        stop.getTimeInMillis() - start.getTimeInMillis());
                return;
            } else {
                Thread.sleep(1);
            }
        }
        throw new RuntimeException("Session not initiated after " + REGISTRATION_TIMEOUT + " ms");
    }

    @Override
    public Option[] getLoggingOptions() {
        Option[] options = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractMdsalTestBase.class),
                        LogLevel.INFO.name()),
        };
        options = ObjectArrays.concat(options, super.getLoggingOptions(), Option.class);
        return options;
    }

}
