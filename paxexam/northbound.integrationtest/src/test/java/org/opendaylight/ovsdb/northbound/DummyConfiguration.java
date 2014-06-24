package org.opendaylight.ovsdb.northbound;

import static org.ops4j.pax.exam.CoreOptions.options;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class DummyConfiguration {
    @Configuration
    public static Option[] config() {
        return options();
    }
}
