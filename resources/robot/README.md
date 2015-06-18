# Purpose
Creates VMs running CentOS 7.0 x64 with OpenVSwitch. 
Please use the Vagrant environment variable OVS_NODES to set the number of VMs that would be created. Default value is 2 (ovs1 and ovs2).

# About the included OVS rpm
To improve provisioning time, "openvswitch-2.3.1-1.x86_64.rpm" is pulled from dropbox. You can add rpm files for other OVS version if desired. Default ovs version is 2.3.1.

To build ovs for the VMs from source, open the vagrant file and make changes to :

Line 19: ovsversion = "<desired_ovs_release>" 
Line 50: puppet.manifest_file  = "ovsnode_build.pp"



