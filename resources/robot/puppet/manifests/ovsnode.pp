  import 'dependencies.pp'

  notice("specified ovs version to install: ${ovsversion}")

  exec { "install_ovs":
    command => "yum localinstall -y /vagrant/shared/openvswitch-${ovsversion}-1.x86_64.rpm",
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
  }

  exec { "start_ovs":
    command => "systemctl start openvswitch.service",
    require => [
                  Exec["install_ovs"],
               ],
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
  }
