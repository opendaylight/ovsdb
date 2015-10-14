define(['app/ovsdb/ovsdb.module'], function(ovsdb) {

  ovsdb.register.constant('nodeIdentifier', {
    IP: 'ip',
    ID: 'node-id',
    REMOTE_PORT: 'remote-port',
    SRC_NODE: 'source-node',
    DEST_NODE: 'dest-node',
    SRC_TP: 'source-tp',
    DEST_TP: 'dest-tp',
    ADDRESSES: 'addresses'
  });

  ovsdb.register.constant('ovsNodeKeys', {
    NODE_ID: 'node-id',
    CONNECTION_INFO: 'ovsdb:connection-info',
    OVS_VERSION: 'ovsdb:ovs-version',
    LOCAL_IP: 'local-ip',
    LOCAL_PORT: 'local-port',
    REMOTE_IP: 'remote-ip',
    REMOTE_PORT: 'remote-port',
    OTHER_CONFIG: 'ovsdb:openvswitch-other-configs',
    OTHER_CONFIG_KEY: 'other-config-key',
    OTHER_CONFIG_VALUE: 'other-config-value'
  });

  ovsdb.register.constant('bridgeNodeKeys', {
    NODE_ID: 'node-id',
    CONTROLLER_ENTRY: 'ovsdb:controller-entry',
    TARGET: 'target',
    IS_CONNECTED: 'is-connected',
    DATA_PATH: 'ovsdb:datapath-id',
    BRIDGE_NAME: 'ovsdb:bridge-name',
    TP: 'termination-point'
  });

  ovsdb.register.constant('tpKeys', {
    NAME: 'ovsdb:name',
    OF_PORT: 'ovsdb:ofport',
    INTERFACE_TYPE: 'ovsdb:interface-type',
    ATTACHED_MAC: 'attached-mac',
    IFACE_ID: 'iface-id',
    EXTERNAL_KEY_ID: 'external-id-key',
    EXTERNAL_KEY_VALUE: 'external-id-value'
  });

  ovsdb.register.constant('flowInfoKeys', {
    FEATURE: 'flow-node-inventory:switch-features',
    SOFTWARE: 'flow-node-inventory:software',
    HARDWARE: 'flow-node-inventory:hardware',
    MANUFACTURER: 'flow-node-inventory:manufacturer',
    IP: 'flow-node-inventory:ip-address',
    TABLE: 'flow-node-inventory:table'
  });

  ovsdb.register.constant('linkIdentifier', {
    SRC: 'source',
    l3_unicast: 'l3-unicast-igp-topology:igp-link-attributes',
    overlay_tunnel_type: 'overlay:tunnel-type',
    supported_link: 'supporting-link',
    ID: 'link-id',
    DEST: 'destination'
  });
});
