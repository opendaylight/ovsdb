#!/usr/bin/python

"""Custom L3 topology

4 hosts directly connected to a bridge instance

Refs: https://www.dropbox.com/s/rh6w9nvkuin7tl9/Screenshot%202014-08-14%2017.53.10.png
https://www.dropbox.com/s/h42ufboknd2i0q1/Screenshot%202014-08-14%2017.52.09.png

host1 -- blue broadcast
host2, host4 -- green broadcast
host3 -- external broadcast

Adding the 'topos' dict with a key/value pair to generate our newly defined
topology enables one to pass in '--topo=mytopo' from the command line.
"""

from mininet.topo import Topo

class L3TestTopo( Topo ):

    """L3 test topology."""

    def __init__( self ):
        """Create custom topo."""

        # Initialize topology
        Topo.__init__( self )

        # Add hosts and switches
        h1 = self.addHost('h1',
                          ip="10.10.10.2/24",
                          mac="00:00:00:00:00:01",
                          defaultRoute="dev h1-eth0 via 10.10.10.1")

        h2 = self.addHost('h2',
                          ip="10.10.20.2/24",
                          mac="00:00:00:00:00:02",
                          defaultRoute="dev h2-eth0 via 10.10.20.1")

        h3 = self.addHost('h3',
                          ip="172.16.1.2/24",
                          mac="00:00:00:00:00:03",
                          defaultRoute="dev h3-eth0 via 172.16.1.254")

        h4 = self.addHost('h4',
                          ip="10.10.20.4/24",
                          mac="00:00:00:00:00:04",
                          defaultRoute="dev h4-eth0 via 10.10.20.1")

        s1 = self.addSwitch('s1')

        # Add links
        self.addLink( s1, h1 )
        self.addLink( s1, h2 )
        self.addLink( s1, h3 )
        self.addLink( s1, h4 )

topos = {'l3': ( lambda: L3TestTopo() )}

