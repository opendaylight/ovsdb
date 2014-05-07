#!/bin/sh
#
# Copyright (C) 2013 Cisco Systems, Inc.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Authors : Thomas Bachman

#
# Generate the local.conf, based on this machine's settings,
# and user input
#

odl_ip=""
odl_in_ds="y"
vm_personality="ODC"
active_ifs=""
service_host=""
service_host_name=""
odl_mgr_ip=""
host_ip=""
host_name=`hostname`
host_list=""

# Minimum memory requirements, in kB
od_mem=50000000
oc_mem=3000000
o_mem=2000000
odoc_mem=5000000
oco_mem=3000000



#
# personality-dependent services
#
ds_common_services="qpid neutron odl-compute n-cpu"
ds_compute_services="nova n-novnc"
ds_controller_services="n-cond q-svc q-dhcp q-l3 q-meta tempest"

#
# everyone gets these services
#
services=$ds_common_services

#
# services that are disabled
#
disabled_services="rabbit n-net"

header='''
LOGFILE=stack.sh.log\n
#SCREEN_LOGDIR=/opt/stack/data/log\n
#LOG_COLOR=False\n
OFFLINE=True\n
#RECLONE=yes\n
\n
'''

#
# single quoted to prevent variable substitutions
#
# add host info

footer='''
Q_HOST=$SERVICE_HOST\n
Q_PLUGIN=ml2\n
Q_ML2_PLUGIN_MECHANISM_DRIVERS=opendaylight,logger\n
ENABLE_TENANT_TUNNELS=True\n
ODL_BOOT_WAIT=70\n
\n
VNCSERVER_PROXYCLIENT_ADDRESS=${HOST_IP}\n
VNCSERVER_LISTEN=0.0.0.0\n
\n
#FLOATING_RANGE=192.168.210.0/24\n
#PUBLIC_NETWORK_GATEWAY=192.168.75.254\n
MYSQL_HOST=$SERVICE_HOST\n
RABBIT_HOST=$SERVICE_HOST\n
GLANCE_HOSTPORT=$SERVICE_HOST:9292\n
KEYSTONE_AUTH_HOST=$SERVICE_HOST\n
KEYSTONE_SERVICE_HOST=$SERVICE_HOST\n
\n
MYSQL_PASSWORD=mysql\n
RABBIT_PASSWORD=rabbit\n
QPID_PASSWORD=rabbit\n
SERVICE_TOKEN=service\n
SERVICE_PASSWORD=admin\n
ADMIN_PASSWORD=admin\n
\n
[[post-config|/etc/neutron/plugins/ml2/ml2_conf.ini]]\n
[agent]\n
minimize_polling=True\n
'''

#
# Create a list of active network interfaces
#
get_active_ifs()
{
   all_ifs=`egrep 'ONBOOT' /etc/sysconfig/network-scripts/ifcfg-*`
   for interface in $all_ifs
   do 
       active=`echo $interface | awk 'BEGIN {FS="="} {print $2}'`
       if [ "$active" == "yes" ]; then
           file=`echo $interface | awk 'BEGIN {FS=":"} {print $1}'`
           intf=`basename $file | awk 'BEGIN {FS="-"} {print $2}'`
           active_ifs=`echo "$active_ifs $intf"`
       fi
   done
}

#
# Have the user select the IP address to use
# 
select_host_ip()
{
    get_active_ifs
    echo "Your system has the following IP addresses: "
    count=0
    if_index="0"
    for interface in $active_ifs
    do
        if [ "$interface" == "lo" ]; then
            continue;
        fi
        exists=`ifconfig | grep $interface`
        if [ "$exists" == "" ]; then
            continue;
        fi
        ip=`ifconfig $interface | grep 'inet ' | awk 'BEGIN {FS=" "} {print $2}'`
        echo "    [$count] $interface: $ip"
        let "count+=1";
    done

    entry=$if_index
    if [ $count == 0 ]; then
        echo "No active interfaces: please reconfigure /etc/sysconfig/network-scripts/ifcfg-<interface> and re-run"
        exit
    elif [ $count > 1 ]; then
        echo -n "Select which interface to use [$entry]: "
        read -a entry
        if [ "$entry" > "$count" ]; then
            let "maxcnt=count-1"
            echo "Invalid entry, must be less than $maxcnt"
            exit
        elif [ "$entry" == "" ]; then
            entry=$if_index
        else
            $if_index=$entry
        fi
        
    fi

    count=0
    for interface in $active_ifs
    do
        if [ $count == $if_index ]; then
            #echo "foo"
            host_ip=`ifconfig $interface | grep 'inet ' | awk 'BEGIN {FS=" "} {print $2}'`
            break;
        fi
        let "count+=1"
    done
}

#
#  Verify the memory is sufficient for the configuration
#
check_mem()
{
    memory=`cat /proc/meminfo  | grep MemTotal | awk 'BEGIN {FS=" "} {print $2}'`
    if [ "$vm_personality" == "O" ]; then
        mem_needed=$od_mem
    elif [ "$vm_personality" == "DC" ]; then
        mem_needed=$oc_mem
    elif [ "$vm_personality" == "D" ]; then 
        mem_needed=$o_mem
    elif [ "$vm_personality" == "ODC" ]; then
        mem_needed=$odoc_mem
    elif [ "$vm_personality" == "DCD" ]; then
        mem_needed=$oco_mem
    fi
    if [ "$memory" -lt "$mem_needed" ]; then
        echo """
             $vm_personality configurations require at least $mem_needed (have $memory).
             Please shutdown the VM and reconfigure its memory
             """
        exit
    fi
}

echo """
   This script configures the local.conf for the
   VM, based on the desired configuration.  The
   VM can take on any of the following roles:
   
       O:    OpenDaylight Controller. 
               In this role, the VM is only responsible
               for running the OpenDaylight Controller

       DC:   DevStack Controller. 
               In this role, the VM is only responsible
               for running the DevStack controller.

       D:    DevStack Compute. In this role,
               the VM is only responsible for 
               running the DevStack compute.

       ODC:  OpenDaylight Controller with DevStack Controller.
               In this role, the VM runs both the OpenDaylight
               controller and the DevStack controller. The 
               OpenDaylight controller is launched by DevStack.

       DCD:  DevStack Controller with DevStack Compute. 
               In this role, the VM acts as both an DevStack
               controller and compute node.
"""

echo -n "Enter VM's personality [$vm_personality]: " 
read -a entry
if [ "$entry" == "" ]; then
    entry=$vm_personality
else
    vm_personality=$entry
fi
if [ "$vm_personality" != "O" ] && 
   [ "$vm_personality" != "DC" ] &&
   [ "$vm_personality" != "D" ]  &&
   [ "$vm_personality" != "ODC" ]  &&
   [ "$vm_personality" != "DCD" ]; then
    echo "$vm_personality is invalid"
    exit;
fi

# For all nodes, run the memory check to make
# sure they've allocated enough memory to
# run the system.
check_mem

# Get the right IP
select_host_ip

service_host=$host_ip
service_host_name=$host_name

# For a combined ODL/DS coontroller, we still need
# to add in the services
if [ "$vm_personality" == "ODC" ]; then 
   services="$services $ds_controller_services odl-server"
fi

# For all pure DS nodes (no ODL -- DS controller, compute, or both), 
# we need to point it to the ODL Controller
if [ "$vm_personality" == "D" ] || 
   [ "$vm_personality" == "DC" ] ||
   [ "$vm_personality" == "DCD" ]; then
   services="$services $ds_controller_services"
   service_host=$host_ip
   service_host_name=$host_name
   echo """
           DevStack nodes need to point to the OpenDaylight Controller. 
           Please provide the IP address of the OpenDaylight Controller.
        """
   echo -n "OpenDaylight Controller IP: " 
   read -a odl_ip
fi


#
# For DS Compute Only, we need to point it to
# the DS Controller and update the DS services
if [ "$vm_personality" == "D" ]; then
   services="$services $ds_compute_services"
   echo """
           DevStack Compute nodes need to point to the DevStack
           Controller. Please provide the IP address and hostname
           of the DevStack Controller
        """
   echo -n "DevStack Controller IP: " 
   read -a service_host
   echo -n "DevStack Controller hostname: " 
   read -a service_host_name
   if [ "$service_host_name" == "$host_name" ]; then
       echo """
               Note: You have configured the DevStack controller's
                     with the same name as this host. If this host
                     requires a name change, run the set_hostname.sh
                     script before running this one.
            """
   fi
   echo """
           DevStack Compute nodes also need to point to the 
           OpenDaylight Controller. Please provide the IP address 
           of the OpenDaylight Controller
        """

   # for DS compute, keep track of service host
   host_list=$service_host_name
fi

######
# Generate local.conf, using parameters
######
echo "[[local|localrc]]" > local.conf

#
#  Header
#
echo -e $header | while read -r line
do 
    echo -e $line >> local.conf
done

#
# Services
#
# disable all services for compute only nodes
if [ "$vm_personality" == "D" ]; then
   echo "disable_all_services" >> local.conf
fi

for line in $disabled_services
do
   echo "disable_service $line" >> local.conf
done

for line in $services
do
   echo -e "enable_service $line" >> local.conf
done
echo -e "\n" >> local.conf



#
# Add in VM-specific parameters
#
echo "HOST_IP=$host_ip" >> local.conf
echo "HOST_NAME=$host_name" >> local.conf
echo "SERVICE_HOST=$service_host" >> local.conf
echo "SERVICE_HOST_NAME=$service_host_name" >> local.conf
if [ "$vm_personality" != "O" ] &&
   [ "$vm_personality" != "ODC" ]; then
    echo "ODL_MGR_IP=$odl_ip" >> local.conf
fi


#
# Footer
#
echo -e $footer | while read -r line
do 
    echo -e $line >> local.conf
done


if [ "$host_list" != "" ]; then
    echo "*********************************************************"
    echo "===========>>>> Be sure to add $host_list to /etc/hosts!"
    echo "*********************************************************"
fi


