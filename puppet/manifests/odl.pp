package { "openjdk-7-jdk":
    ensure => "installed"
}

package { "maven":
    ensure => "installed"
}

file_line {"M2_HOME":
    ensure => present,
    path => "/home/vagrant/.bashrc",
    line => "export M2_HOME=/usr/share/maven",
    before => Exec["mvn clean install"]
}

file_line {"JAVA_HOME":
    ensure => present,
    path => "/home/vagrant/.bashrc",
    line => "export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-amd64"
}

file { "/home/vagrant/src":
  ensure => 'directory',
  source => "file:///vagrant",
  owner => 'vagrant',
  group => 'vagrant',
  recurse => true,
  ignore => ".*"
}

exec { "mvn clean install":
    cwd => "/home/vagrant/src",
    path => "/usr/local/bin/:/usr/bin:/bin/",
    user => vagrant,
    timeout => 0,
    require => File["/home/vagrant/src"]
}

file { "/home/vagrant/dist":
    ensure => 'link',
    target => "src/distribution/opendaylight/target/distribution.ovsdb-1.0.1-SNAPSHOT-osgipackage/opendaylight",
    owner => 'vagrant',
    group => 'vagrant',
    require => Exec["mvn clean install"]
}
