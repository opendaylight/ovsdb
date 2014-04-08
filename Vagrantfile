# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.provision "shell", path: "resources/puppet/scripts/bootstrap.sh"

  config.vm.provision "puppet" do |puppet|
      puppet.hiera_config_path = "resources/puppet/hiera.yaml"
      puppet.working_directory = "/vagrant/resources/puppet"
      puppet.manifests_path = "resources/puppet/manifests"
      puppet.manifest_file  = "base.pp"
  end

  config.vm.define "odl" do |odl|
    odl.vm.box = "saucy64"
    odl.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/virtualbox/opscode_ubuntu-13.10_chef-provisionerless.box"
    odl.vm.hostname = "opendaylight"
    odl.vm.network "private_network", ip: "192.168.50.10"
    odl.vm.network "forwarded_port", guest: 8080, host: 8080
    odl.vm.network "forwarded_port", guest: 8000, host: 8000
    odl.vm.provider :virtualbox do |vb|
      vb.memory = 2048
    end
    odl.vm.provision "puppet" do |puppet|
      puppet.hiera_config_path = "resources/puppet/hiera.yaml"
      puppet.working_directory = "/vagrant/resources/puppet"
      puppet.manifests_path = "resources/puppet/manifests"
      puppet.manifest_file  = "odl.pp"
    end
  end

  config.vm.define "mininet" do |mininet|
    mininet.vm.box = "saucy64"
    mininet.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/virtualbox/opscode_ubuntu-13.10_chef-provisionerless.box"
    mininet.vm.hostname = "mininet"
    mininet.vm.network "private_network", ip: "192.168.50.15"
    mininet.vm.provider :virtualbox do |vb|
      vb.memory = 2048
    end
    mininet.vm.provision "puppet" do |puppet|
      puppet.hiera_config_path = "resources/puppet/hiera.yaml"
      puppet.working_directory = "/vagrant/resources/puppet"
      puppet.manifests_path = "resources/puppet/manifests"
      puppet.manifest_file  = "mininet.pp"
    end
  end

  config.vm.define "devstack-control" do |dsctl|
    dsctl.vm.box = "saucy64"
    dsctl.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/virtualbox/opscode_ubuntu-13.10_chef-provisionerless.box"
    dsctl.vm.hostname = "devstack-control"
    dsctl.vm.network "private_network", ip: "192.168.50.20"
    odl.vm.network "forwarded_port", guest: 8080, host: 8081
    dsctl.vm.provider :virtualbox do |vb|
      vb.memory = 4096
    end
    dsctl.vm.provision "puppet" do |puppet|
      puppet.hiera_config_path = "resources/puppet/hiera.yaml"
      puppet.working_directory = "/vagrant/resources/puppet"
      puppet.manifests_path = "resources/puppet/manifests"
      puppet.manifest_file  = "devstack-control.pp"
    end
  end

  config.vm.define "devstack-compute" do |dscom|
    dscom.vm.box = "saucy64"
    dscom.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/virtualbox/opscode_ubuntu-13.10_chef-provisionerless.box"
    dscom.vm.hostname = "devstack-compute"
    dscom.vm.network "private_network", ip: "192.168.50.21"
    dscom.vm.provider :virtualbox do |vb|
      vb.memory = 4096
    end
    dscom.vm.provision "puppet" do |puppet|
      puppet.hiera_config_path = "resources/puppet/hiera.yaml"
      puppet.working_directory = "/vagrant/resources/puppet"
      puppet.manifests_path = "resources/puppet/manifests"
      puppet.manifest_file  = "devstack-compute.pp"
    end
  end

end
