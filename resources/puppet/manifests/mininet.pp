$deps = [ 'build-essential',
          'debhelper',
          'dkms',
          'fakeroot',
          'graphviz',
          'libssl-dev',
          'linux-headers-generic',
          'python-all',
          'python-qt4',
          'python-zopeinterface',
          'python-twisted-conch',
          'python-twisted-web',
          'xauth'
]

package { $deps:
    ensure   => installed,
}

vcsrepo { '/home/vagrant/mininet':
    ensure   => present,
    provider => git,
    user     => 'vagrant',
    source   => 'git://github.com/mininet/mininet',
    revision => '2.1.0p2',
    before   => Exec['Install Mininet']
}

exec { 'Install Mininet':
    command => 'bash mininet/util/install.sh -nf > /dev/null',
    cwd     => '/home/vagrant',
    user    => 'vagrant',
    path    => $::path,
    timeout => 0
}

exec {'openvswitch-2.1.2.tar.gz':
    command => 'wget http://openvswitch.org/releases/openvswitch-2.1.2.tar.gz',
    cwd     => '/home/vagrant',
    path    => $::path,
    user    => 'vagrant'
}

exec { 'Extract Open vSwitch':
    command => 'tar -xvf openvswitch-2.1.2.tar.gz',
    cwd     => '/home/vagrant',
    user    => 'vagrant',
    path    => $::path,
    timeout => 0,
    require => Exec['openvswitch-2.1.2.tar.gz']
}

exec { 'Compile Open vSwitch':
    command => 'fakeroot debian/rules binary',
    cwd     => '/home/vagrant/openvswitch-2.1.2',
    user    => 'root',
    path    => $::path,
    timeout => 0,
    require => [Exec['Extract Open vSwitch'], Package[$deps]]
}

package { 'openvswitch-common':
    ensure   => installed,
    provider => dpkg,
    source   => '/home/vagrant/openvswitch-common_2.1.2-1_amd64.deb',
    require  => Exec['Compile Open vSwitch']
}

package { 'openvswitch-switch':
    ensure   => installed,
    provider => dpkg,
    source   => '/home/vagrant/openvswitch-switch_2.1.2-1_amd64.deb',
    require  => Package['openvswitch-common']
}

package { 'openvswitch-datapath-dkms':
    ensure   => installed,
    provider => dpkg,
    source   => '/home/vagrant/openvswitch-datapath-dkms_2.1.2-1_all.deb',
    require  => Package['openvswitch-switch']
}

package { 'openvswitch-pki':
    ensure   => installed,
    provider => dpkg,
    source   => '/home/vagrant/openvswitch-pki_2.1.2-1_all.deb',
    require  => Package['openvswitch-datapath-dkms']
}

exec { 'Compile Test Controller':
    command  => 'sh boot.sh && sh configure && make',
    cwd      => '/home/vagrant/openvswitch-2.1.2',
    path     => $::path,
    user     => 'root',
    require  => [Exec['Compile Open vSwitch'], Package[$deps]]
}

exec { 'Link Test Controller':
    command  => 'ln -s /home/vagrant/openvswitch-2.1.2/tests/test-controller /usr/bin/ovs-controller',
    cwd      => '/home/vagrant/openvswitch-2.1.2',
    path     => $::path,
    user     => 'root',
    require  => Exec['Compile Test Controller']
}
