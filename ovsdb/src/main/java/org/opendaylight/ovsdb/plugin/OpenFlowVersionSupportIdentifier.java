package org.opendaylight.ovsdb.plugin;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenFlowVersionSupportIdentifier {
    static final Logger logger = LoggerFactory.getLogger(OpenFlowVersionSupportIdentifier.class);
    private static int supportedOpenflowVersion = 0;

    protected static final String OPENFLOW_10 = "1.0";
    protected static final String OPENFLOW_13 = "1.3";

    private static OpenFlowVersionSupportIdentifier identifier = new OpenFlowVersionSupportIdentifier();
    private OpenFlowVersionSupportIdentifier() {
    }

    public static int getSupportedOpenflowVersion() {
        return identifier.getSupportedVersion();
    }

    private int getSupportedVersion() {
        String ofVersion = System.getProperty("ovsdb.of.version", "");
        switch (ofVersion) {
            case OPENFLOW_13:
                supportedOpenflowVersion = 4;
            case OPENFLOW_10:
                supportedOpenflowVersion = 1;
            default:
                break;
        }

        if (supportedOpenflowVersion == 0) {
            BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                    .getBundleContext();
            for(Bundle bundle : bundleContext.getBundles()){
                 if (bundle.getSymbolicName().contains("protocol_plugins.openflow")) {
                     logger.debug("=========OPENFLOW BUNDLE FOUND : "+ bundle.getSymbolicName()+"==========");
                     supportedOpenflowVersion = 1;
                     break;
                 } else if (bundle.getSymbolicName().contains("openflowplugin")) {
                     logger.debug("=========OPENFLOW BUNDLE FOUND : "+ bundle.getSymbolicName()+"==========");
                     supportedOpenflowVersion = 4;
                     break;
                 }
            }
        }
        return supportedOpenflowVersion;
    }
}
