  notice("specified ovs version to install: ${ovsversion}")
  
  exec { "mktarget":
    command => "/bin/mkdir -p /vagrant/shared/",
    cwd     => "/",
  }

  file { [           
           "/vagrant/shared/",
         ]:
    ensure => directory,
  }

  exec { "download_ovs":
    command => "/usr/bin/wget https://www.dropbox.com/s/v34fbw6t0mvtoi8/openvswitch-${ovsversion}-1.x86_64.rpm?dl=0 -O /vagrant/shared/openvswitch-${ovsversion}-1.x86_64.rpm",
    cwd     => "/root",
    creates => "/vagrant/shared/openvswitch-${ovsversion}-1.x86_64.rpm",
    require => [
                  Exec["mktarget"],
               ],
  }

  exec { "install_ovs":
    command => "yum localinstall -y /vagrant/shared/openvswitch-${ovsversion}-1.x86_64.rpm",
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
    require => [
                  Exec["download_ovs"],
               ],
  }

  exec { "start_ovs":
    command => "systemctl start openvswitch.service",
    require => [
                  Exec["install_ovs"],
               ],
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
  }
