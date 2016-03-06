vcsrepo { "/home/vagrant/devstack":
    provider => git,
    ensure => present,
    user => "vagrant",
    source => "https://github.com/openstack-dev/devstack.git",
    before => File['/home/vagrant/devstack/local.conf']
}

$hosts = hiera('hosts')

file { "/home/vagrant/devstack/local.conf":
    ensure => present,
    owner => "vagrant",
    group => "vagrant",
    content => template('/vagrant/resources/puppet/templates/control.local.conf.erb')
}
