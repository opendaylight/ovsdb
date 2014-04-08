vcsrepo { "/home/vagrant/mininet":
    provider => git,
    ensure => present,
    user => "vagrant",
    source => "git://github.com/mininet/mininet",
    revision => '2.1.0p1',
    before => Exec['Install Mininet']
}

exec { "Install Mininet":
    command => "/bin/bash mininet/util/install.sh -nfv > /dev/null",
    cwd => '/home/vagrant',
    user => 'vagrant',
    timeout => 0
}
