package org.opendaylight.ovsdb.plugin;

import static org.ops4j.pax.exam.CoreOptions.options;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class DummyConfiguration {
    @Configuration
    public Option[] config() {
        return options();
    }
}
