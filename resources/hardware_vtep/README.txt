Notes for configuring the hardware_vtep schema on OVS:

Pre-requisite:
--------------
Fedora 20

Configure:
----------
sudo yum -y install openvswitch python-openvswitch
sudo service openvswitch stop

sudo ovsdb-tool create /etc/openvswitch/conf.db /usr/share/openvswitch/vswitch.ovsschema
sudo ovsdb-tool create /etc/openvswitch/vtep.db /usr/share/openvswitch/vtep.ovsschema

sudo mkdir -p /var/run/openvswitch
sudo mkdir -p /var/log/openvswitch

sudo ovsdb-server --pidfile --log-file --detach \
--remote=punix:/var/run/openvswitch/db.sock \
--remote=ptcp:6640 \
/etc/openvswitch/conf.db /etc/openvswitch/vtep.db

sudo ovs-vswitchd --log-file --detach \
--pidfile unix:/var/run/openvswitch/db.sock

sudo ovs-vsctl add-br br0
sudo ovs-vsctl add-port br0 eth0
sudo vtep-ctl add-ps br0
sudo vtep-ctl add-port br0 eth0
sudo vtep-ctl set Physical_Switch br0 tunnel_ips=192.168.0.3

sudo /usr/share/openvswitch/scripts/ovs-vtep \
--log-file=/var/log/openvswitch/ovs-vtep.log \
--pidfile=/var/run/openvswitch/ovs-vtep.pid --detach br0

sudo vtep-ctl set-manager ptcp:6640

Verify:
-------
sudo ovsdb-client dump hardware_vtep
sudo vtep-ctl list-ps
sudo vtep-ctl list-ports br0

Integration-test:
-----------------
cd ovsdb/integrationtest
mvn verify -Pintegrationtest -Dovsdbserver.ipaddress=192.168.120.31 -Dovsdbserver.port=6640 clean install

Partial output:
17:09:58.055 [main] TRACE o.o.o.lib.jsonrpc.JsonRpcEndpoint - {"id":"7649b44e-0982-44cb-9306-6072247bdafc","method":"list_dbs","params":[]}
17:09:58.099 [nioEventLoopGroup-2-1] TRACE o.o.o.lib.jsonrpc.JsonRpcEndpoint - Response : {"id":"7649b44e-0982-44cb-9306-6072247bdafc","result":["hardware_vtep","Open_vSwitch"],"error":null}
17:09:58.146 [main] TRACE o.o.o.lib.jsonrpc.JsonRpcEndpoint - {"id":"14f7f818-a2a1-40ba-a7ca-1dc7bdabe095","method":"get_schema","params":["hardware_vtep"]}
