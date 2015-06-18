# Purpose
Creates 2 VMs running CentOS 7.0 x64 with OpenVSwitch
- OVS1
- OVS2

# About the included OVS rpm
To improve provisioning speed, "openvswitch-2.3.1-1.x86_64.rpm" is preloaded in the shared folder.
You can add rpm files for other releases if desired.

To build ovs from source, open puppet/manifest/(ovs1.pp & ovs2.pp) and replace the import with this one

import 'ovsnode_build.pp'

Don't forget to change the line (26) below in your Vagrantfile to reflect the correct version of ovs

ovsversion = "2.3.1"


