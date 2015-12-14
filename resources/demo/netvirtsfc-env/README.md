#SETUP

This is a demonstration / development environment for show-casing OpenDaylight OVSDB NETVIRT with ServiceFunctionChaining (SFC)

git clone https://github.com/flavio-fernandes/netvirtsfc-env.git

This demo setup can also be found under the the ovsdb repo of the Opendaylight project:

```
https://github.com/opendaylight/ovsdb/tree/master/resources/demo/netvirtsfc-env
```

This demo is analogous to the demo done by the group-based-policy project of Opendaylight. In fact, the kudos
for initially putting it together goes to our friends Keith, Thomas, and others responsible for GBP:

```
https://github.com/alagalah/gbpsfc-env
```

The initial installation may take some time, with vagrant and docker image downloads. 

After the first time it is very quick.

1. Set up Vagrant. 
  * Edit env.sh for NUM_NODES. (Keep all other vars the same for this version)
    Also set 'ODL_ROOT_DIR' to point to the directory ./openstack/net-virt-sfc/karaf/target/assembly

    That directory will be available when you build the ovsdb project, or where the karaf distro
    got unzipped.

  * Each VM takes approximately 1G RAM, 2GB used HDD (40GB)

  * demo-asymmetric-chain: 6 VMs.

2. From the 'netvirtsfc-env' directory do:
```
source ./env.sh
vagrant up
```
  * This takes quite some time initially. 

3. Start controller.
  * Currently it is expected that that controller runs on the machine hosting the vagrant VMs.
  * Tested using ovsdb netvirt beryllium.

  * Set config for your setup:

    Use the script 'setsfc.sh' to make the changes below. You only need to do it once after build.

    * Modify the NetvirtSfc config.xml to start in standalone mode. (i.e. set of13provider to standalone)
    * Modify the logging levels to help with troubleshooting
    * Start ODL with the following feature loaded:  odl-ovsdb-sfc-ui

  * Start controller by running bin/karaf and make sure the following features are installed
```
    cd $ODL_ROOT_DIR ; ./bin/karaf
```

```
    opendaylight-user@root>feature:list -i | grep ovsdb-sfc
    odl-ovsdb-sfc-test                   | 1.2.1-SNAPSHOT   | x         | odl-ovsdb-sfc-test1.2.1-SNAPSHOT        | OpenDaylight :: ovsdb-sfc-test
    odl-ovsdb-sfc-api                    | 1.2.1-SNAPSHOT   | x         | odl-ovsdb-sfc-1.2.1-SNAPSHOT            | OpenDaylight :: ovsdb-sfc :: api
    odl-ovsdb-sfc                        | 1.2.1-SNAPSHOT   | x         | odl-ovsdb-sfc-1.2.1-SNAPSHOT            | OpenDaylight :: ovsdb-sfc
    odl-ovsdb-sfc-rest                   | 1.2.1-SNAPSHOT   | x         | odl-ovsdb-sfc-1.2.1-SNAPSHOT            | OpenDaylight :: ovsdb-sfc :: REST
    odl-ovsdb-sfc-ui                     | 1.2.1-SNAPSHOT   | x         | odl-ovsdb-sfc-1.2.1-SNAPSHOT            | OpenDaylight :: ovsdb-sfc :: UI
```

    Note that if you missed running 'setsfc.sh' ODL will operate in non-standalone mode, which is going
    to make ovsdb netvirt work with openstack/tacker environments.

  * Run `log:tail | grep SfcL2Renderer` and wait until the following message appears in the log:
```
 successfully started the SfcL2Renderer plugin
```
  * Now you can ^C the log:tail if you wish

##demo-asymmetric-chain

  * Service Chain classifying HTTP traffic.
  * Traffic in the forward direction is chained and in the reverse direction the traffic uses the normal VxLAN tunnel
  * 2 docker containers in the same tenant space

![asymmetric-chain demo diag](https://raw.githubusercontent.com/flavio-fernandes/netvirtsfc-env/master/images/asymmetric-sfc-demo.png)

VMs:
* netvirtsfc1: netvirt (client initiates transactions from here)
* netvirtsfc2: sff
* netvirtsfc3: "sf"
* netvirtsfc4: sff
* netvirtsfc5: "sf"
* netvirtsfc6: netvirt (run a server here)

Containers:
* h35_2 is on netvirtsfc1. This host serves as the client.
* h35_4 is netvirtsfc6. This host serves as the webserver.

To run, from host folder where Vagrantfile located do:

`./startdemo.sh demo-asymmetric-chain`

### To test by sending traffic:
Start a test HTTP server on h35_4 in VM 6.

*(don't) forget double ENTER after `docker attach`*
```bash
vagrant ssh netvirtsfc6
docker ps
docker attach h35_4
python -m SimpleHTTPServer 80
```

Ctrl-P-Q to detach from docker without stopping the SimpleHTTPServer, and logoff netvirtsfc6.

Now start client traffic, either ping or make HTTP requests to the server on h36_4.

```bash
vagrant ssh netvirtsfc1
docker ps
docker attach h35_2
ping 10.0.35.4
curl 10.0.35.4
while true; do curl 10.0.35.4; sleep 1; done
```

Ctrl-P-Q to detach from docker, leaving the client making HTTP requests, and logoff netvirtsfc1.

Look around: use "vagrant ssh" to the various machines. To run wireshark, ssh to the vms using the -XY flags:
```
vagrant ssh netvirtsfcX -- -XY   (e.g.: vagrant ssh netvirtsfc1 -- -XY)
sudo wireshark &
```

 * take packet captures on eth1, as that is the interface used for communication between vms.
 * sudo ovs-dpctl dump-flows


### When finished from host folder where Vagrantfile located do:

`./cleandemo.sh`

If you like `vagrant destroy` will remove all VMs

##Preparing to run another demo
1. In the vagrant directory, run cleandemo.sh
2. stop controller (logout of karaf)
3. Remove data, journal and snapshot directories from controller directory.
4. Restart tests starting with restarting the controller, install features, wait, as above.
