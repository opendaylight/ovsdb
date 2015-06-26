# Purpose
1)
Creates VMs running CentOS 7.0 x64 with OpenVSwitch.
Please use the Vagrant environment variable OVS_NODES to set the number of VMs that would be created. Default value is 2 (ovs1 and ovs2).

2)
Sets up Robot framework in the first VM (ovs1). Subsequent VMs are will only have OVS

# About the included OVS rpm
To improve provisioning time, "openvswitch-2.3.1-1.x86_64.rpm" is pulled from dropbox. You can add rpm files for other OVS version if desired. Default ovs version is 2.3.1.

To build ovs for the VMs from source, open the vagrant file and make changes to :

Line 19: ovsversion = "<desired_ovs_release>"
Line 50: puppet.manifest_file  = "ovsnode_build.pp"

# Running integration tests for OVSDB

After the VMs are provisioned. ssh into ovs1 to run integration tests for OVSDB

        user@machine$ vagrant ssh ovs1
        vagrant@ovs1:~\> sh run_robot_tests.sh

# OpenDaylight Controller

The controller should be running on the host machine before you run the integration tests. The VMs are setup with environmental variable $CONTROLLER with the default IP: 192.168.100.1

# Output and log from each test

The output and logs for each test will be left in ovs1 home directory. For convinience of accessing the test results at a later time from the host machine, check "robot/scripts/results" for the result of the current and previous tests. Those are timestamped and cumulated over time. You are responsible for cleaning up this cache.

# To run specific patches

This script will automatically download the latest version of the master branch of the integration project. If you need to test a specific patch, open run_robot_tests.sh on the home directory of ovs1

Edit the git clone url as desired

        sudo git clone https://git.opendaylight.org/gerrit/integration

# To run integration tests for other projects

If this is temporary, edit line 17 of run_robot_tests.sh in the home directory of ovs1.

        export test_suite_dir="$HOME/integration/test/csit/suites/ovsdb/"

For a permanent change, make the edits described above in the version of this file in robot/scripts and re-provision your VM.
