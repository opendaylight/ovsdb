#!/usr/bin/python

import socket
import os
import re
import time
import sys
import ipaddr
import commands
from subprocess import call
from subprocess import check_output
from infrastructure_config import *

def addController(sw, ip):
    call(['ovs-vsctl', 'set-controller', sw, 'tcp:%s:6653' % ip ])

def addManager(ip):
    cmd="ovs-vsctl set-manager tcp:%s:6640" % ip
    listcmd=cmd.split()
    print check_output(listcmd)

def addSwitch(name, dpid=None):
    call(['ovs-vsctl', 'add-br', name]) #Add bridge
    if dpid:
        if len(dpid) < 16: #DPID must be 16-bytes in later versions of OVS
            filler='0000000000000000'
            dpid=filler[:len(filler)-len(dpid)]+dpid
        elif len(dpid) > 16:
            print 'DPID: %s is too long' % dpid
            sys.exit(3)
        call(['ovs-vsctl','set','bridge', name,'other-config:datapath-id=%s'%dpid])

def addHost(net, switch, name, ip, mac):
    containerID=launchContainer()

#,OpenFlow12,OpenFlow10
def setOFVersion(sw, version='OpenFlow13'):
    call(['ovs-vsctl', 'set', 'bridge', sw, 'protocols={}'.format(version)])

def addTunnel(sw, port, sourceIp=None, remoteIp=None):
    ifaceName = '{}-vxlan-0'.format(sw)
    cmd = ['ovs-vsctl', 'add-port', sw, ifaceName,
           '--', 'set', 'Interface', ifaceName,
           'type=vxlan',
           'options:local_ip=%s'%sourceIp,
           'options:remote_ip=%s'%remoteIp,
           'options:key=4096',
           'ofport_request=%s'%port]
#    if sourceIp is not None:
#        cmd.append('options:source_ip={}'.format(sourceIp))
    call(cmd)

def addGpeTunnel(sw, sourceIp=None):
    ifaceName = '{}-vxlangpe-0'.format(sw)
    cmd = ['ovs-vsctl', 'add-port', sw, ifaceName,
           '--', 'set', 'Interface', ifaceName,
           'type=vxlan',
           'options:remote_ip=flow',
           'options:dst_port=6633',
           'options:nshc1=flow',
           'options:nshc2=flow',
           'options:nshc3=flow',
           'options:nshc4=flow',
           'options:nsp=flow',
           'options:nsi=flow',
           'options:key=flow',
           'ofport_request=7']
#    if sourceIp is not None:
#        cmd.append('options:source_ip={}'.format(sourceIp))
    call(cmd)

def launchContainer(host,containerImage):
    containerID= check_output(['docker','run','-d','--net=none','--name=%s'%host['name'],'-h',host['name'],'-t', '-i','--privileged=True',containerImage,'/bin/bash']) #docker run -d --net=none --name={name} -h {name} -t -i {image} /bin/bash
    #print "created container:", containerID[:-1]
    return containerID[:-1] #Remove extraneous \n from output of above

def connectContainerToSwitch(sw,host,containerID,of_port):
    hostIP=host['ip']
    mac=host['mac']
    nw = ipaddr.IPv4Network(hostIP)
    broadcast = "{}".format(nw.broadcast)
    router = "{}".format(nw.network + 1)
    cmd=['/vagrant/ovswork.sh',sw,containerID,hostIP,broadcast,router,mac,of_port,host['name']]
    if host.has_key('vlan'):
        cmd.append(host['vlan'])
    call(cmd)

def doCmd(cmd):
    listcmd=cmd.split()
    print check_output(listcmd)

def launch(switches, hosts, contIP='127.0.0.1'):

    for sw in switches:
        addManager(contIP)
        ports=0
        first_host=True
        for host in hosts:
            if host['switch'] == sw['name']:
                if first_host:
                    dpid=sw['dpid']
                    addSwitch(sw['name'],sw['dpid'])
                    setOFVersion(sw['name'])
                    addController(sw['name'], contIP)
                    addGpeTunnel(sw['name'])
                    if host['switch'] == "sw1":
                        addTunnel(sw['name'], 5, "192.168.50.70", "192.168.50.75")
                    if host['switch'] == "sw6":
                        addTunnel(sw['name'], 5, "192.168.50.75", "192.168.50.70")
                first_host=False
                containerImage=defaultContainerImage #from Config
                if host.has_key('container_image'): #from Config
                    containerImage=host['container_image']
                containerID=launchContainer(host,containerImage)
                ports+=1
                connectContainerToSwitch(sw['name'],host,containerID,str(ports))
                host['port-name']='vethl-'+host['name']
                print "Created container: %s with IP: %s. Connect using 'docker attach %s', disconnect with ctrl-p-q." % (host['name'],host['ip'],host['name'])

if __name__ == "__main__" :
#    print "Cleaning environment..."
#    doCmd('/vagrant/clean.sh')
    sw_index=int(socket.gethostname().split("netvirtsfc",1)[1])-1
    if sw_index in range(0,len(switches)+1):

       controller=os.environ.get('ODL')
       sw_type = switches[sw_index]['type']
       sw_name = switches[sw_index]['name']
       if sw_type == 'netvirt':
           print "*****************************"
           print "Configuring %s as a NETVIRT node." % sw_name
           print "*****************************"
           print
           launch([switches[sw_index]],hosts,controller)
           print "*****************************"
           doCmd('sudo /vagrant/utils/overlay-flows.sh')
           print "*****************************"
           print "OVS status:"
           print "-----------"
           print
           doCmd('ovs-vsctl show')
           doCmd('ovs-ofctl -O OpenFlow13 dump-flows %s'%sw_name)
           print
           print "Docker containers:"
           print "------------------"
           doCmd('docker ps')
           print "*****************************"
       elif sw_type == 'sff':
           print "*****************************"
           print "Configuring %s as an SFF." % sw_name
           print "*****************************"
           doCmd('sudo ovs-vsctl set-manager tcp:%s:6640' % controller)
           time.sleep(1)
           dpid=switches[sw_index]['dpid']
           addSwitch(sw_name,dpid)
           setOFVersion(sw_name)
           addController(sw_name, controller)
           #addGpeTunnel(sw_name)
           #doCmd('sudo ovs-vsctl set-manager tcp:%s:6640' % controller)
           print
       elif sw_type == 'sf':
           print "*****************************"
           print "Configuring %s as an SF." % sw_name
           print "*****************************"
           doCmd('sudo /vagrant/sf-config.sh')
           #addGpeTunnel(switches[sw_index]['name'])

