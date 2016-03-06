  import 'dependencies.pp'

  notice("specified ovs version to install: ${ovsversion}")

  exec { "mksource":
    command => "/bin/mkdir -p /rpmbuild/SOURCES/",
    cwd     => "/",
  }

  file { [
           "/rpmbuild/",
           "/rpmbuild/SOURCES/",
         ]:
    ensure => directory,
  }

  exec { "download_ovs":
    command => "wget http://openvswitch.org/releases/openvswitch-${ovsversion}.tar.gz -O /root/openvswitch-${ovsversion}.tar.gz",
    creates => "/root/openvswitch-${ovsversion}.tar.gz",
    cwd    => "/root",
    path    => ["/bin", "/usr/bin"],
  }

  exec { "check_presence":
    command => "true",
    onlyif  => "/usr/bin/test -e /rpmbuild/SOURCES",
    path    => ["/bin", "/usr/bin"],
  }

  exec { "copy_archive":
    command => "cp /root/openvswitch-${ovsversion}.tar.gz /rpmbuild/SOURCES/openvswitch-${ovsversion}.tar.gz",
    require => [
                  Exec["mksource"],
                  Exec["download_ovs"],
                  Exec["check_presence"],
               ],
    cwd    => "/root",
    creates => "/rpmbuild/SOURCES/openvswitch-${ovsversion}.tar.gz",
    path    => ["/bin", "/usr/bin"],
  }

  exec { "extract_ovs":
    command => "tar xvfz /root/openvswitch-${ovsversion}.tar.gz",
    require => [
                  Exec["copy_archive"],
               ],
    path    => ["/bin", "/usr/bin"],
    cwd     => "/root",
    creates => "/root/openvswitch-${ovsversion}/README",
  }

  exec { "custom_sed":
    command => "sed 's/openvswitch-kmod, //g' /root/openvswitch-${ovsversion}/rhel/openvswitch.spec > /root/openvswitch-${ovsversion}/rhel/openvswitch_no_kmod.spec",
    require => [
                  Exec["extract_ovs"],
               ],
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
  }

  exec { "build_ovs":
    command => "rpmbuild -bb --nocheck /root/openvswitch-${ovsversion}/rhel/openvswitch_no_kmod.spec",
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
    require => [
                  Exec["custom_sed"],
               ],
    timeout     => 0,
    creates => "/root/rpmbuild/RPMS/x86_64/openvswitch-${ovsversion}-1.x86_64.rpm",
  }

  exec { "install_ovs":
    command => "yum localinstall -y /rpmbuild/RPMS/x86_64/openvswitch-${ovsversion}-1.x86_64.rpm",
    cwd => "/root",
    path    => ["/bin", "/usr/bin"],
    require => [
                  Exec["build_ovs"],
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
