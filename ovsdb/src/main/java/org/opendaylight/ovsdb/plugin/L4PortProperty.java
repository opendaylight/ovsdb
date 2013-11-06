
package org.opendaylight.ovsdb.plugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Property;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class L4PortProperty extends Property implements Cloneable {
    private static final long serialVersionUID = 1L;
    @XmlElement(name="value")
    private final int port;
    public static final String name = "Port";

    /*
     * Private constructor used for JAXB mapping
     */
    private L4PortProperty() {
        super(name);
        this.port = 0;
    }

    public L4PortProperty(int port) {
        super(name);
        this.port = port;
    }

    @Override
    public String getStringValue() {
        return port+"";
    }

    @Override
    public Property clone() {
        return new L4PortProperty(port);
    }

    public int getPort() {
        return port;
    }
}
