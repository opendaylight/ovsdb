# add a bridge
sudo ovs-vsctl add-br ovs-br0

# add a port to the bridge
#sudo ovs-vsctl add-port ovs-br0 eth1

# link the bridge the controller
sudo ovs-vsctl set-controller ovs-br0 tcp:$CONTROLLER:6633

# link the controller as a manager
sudo ovs-vsctl set-manager tcp:$CONTROLLER:6640
