package org.opendaylight.ovsdb.neutron.mocks;

import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;

import java.util.List;

/**
 * Created by dave on 02/01/2014.
 */
public class DummyNeutronPortCRUD implements INeutronPortCRUD {
    @Override
    public boolean portExists(String s) {
        return false;
    }

    @Override
    public NeutronPort getPort(String s) {
        NeutronPort port = new NeutronPort();

        if(s == "testPortId") {
            port.setNetworkUUID("testNetworkUUID");
        }
        else if (s == "nullPortId") {
            return null;
        }
        else {
            port.setNetworkUUID("fakeNetworkUUID");
        }
        return port;

    }

    @Override
    public List<NeutronPort> getAllPorts() {
        return null;
    }

    @Override
    public boolean addPort(NeutronPort neutronPort) {
        return false;
    }

    @Override
    public boolean removePort(String s) {
        return false;
    }

    @Override
    public boolean updatePort(String s, NeutronPort neutronPort) {
        return false;
    }

    @Override
    public boolean macInUse(String s) {
        return false;
    }

    @Override
    public NeutronPort getGatewayPort(String s) {
        return null;
    }
}
