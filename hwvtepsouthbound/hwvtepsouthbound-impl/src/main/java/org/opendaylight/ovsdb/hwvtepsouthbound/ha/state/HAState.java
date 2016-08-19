package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

public enum HAState {
    NonHA,
    D1Connected,
    D1ReConnected,
    D2Connected,
    D2Reconnected,
    D1Disconnected,
    D2Disconnected;
}
