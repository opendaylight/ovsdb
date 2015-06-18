  $base_packages = [
    "kernel-headers",
    "kernel-devel",
    "gcc",
    "make",
    "python-devel",
    "openssl-devel",
    "graphviz",
    "kernel-debug-devel",
    "automake",
    "rpm-build",
    "redhat-rpm-config",
    "libtool",
    "git"
  ]

  package { $base_packages:
    ensure => installed,
  }

  notice("Dependencies are ready!")
