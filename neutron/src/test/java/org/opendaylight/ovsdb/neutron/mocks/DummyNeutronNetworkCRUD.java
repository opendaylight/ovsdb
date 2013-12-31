package org.opendaylight.ovsdb.neutron.mocks;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;

import java.util.LinkedList;
import java.util.List;

public class DummyNeutronNetworkCRUD implements INeutronNetworkCRUD {
    @Override
    public boolean networkExists(String s) {
        return false;
    }

    @Override
    public NeutronNetwork getNetwork(String s) {
        NeutronNetwork network = new NeutronNetwork();
        network.setNetworkUUID(s);
        return network;
    }

    @Override
    public List<NeutronNetwork> getAllNetworks() {
        List<NeutronNetwork> result = new LinkedList<NeutronNetwork>();
        NeutronNetwork network = new NeutronNetwork();
        network.setProviderSegmentationID("testSegmentationId");
        network.setNetworkUUID("testNetworkUUID");

        result.add(network);

        return result;
    }

    @Override
    public boolean addNetwork(NeutronNetwork neutronNetwork) {
        return false;
    }

    @Override
    public boolean removeNetwork(String s) {
        return false;
    }

    @Override
    public boolean updateNetwork(String s, NeutronNetwork neutronNetwork) {
        return false;
    }

    @Override
    public boolean networkInUse(String s) {
        return false;
    }
}
