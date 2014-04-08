package {"git":
    ensure => "installed"
}

$hosts = hiera('hosts')

file { "/etc/hosts":
    ensure => file,
    owner => "root",
    group => "root",
    content => template('/vagrant/resources/puppet/templates/hosts.erb')
}
