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

  config.vm.define "mininet" do |mininet|
    mininet.vm.box = "saucy64"
    mininet.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/virtualbox/opscode_ubuntu-13.10_chef-provisionerless.box"
    mininet.vm.provider "vmware_fusion" do |v, override|
      override.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/vmware/opscode_ubuntu-13.10_chef-provisionerless.box"
    end
    mininet.vm.hostname = "mininet"
    mininet.vm.network "private_network", ip: "192.168.50.15"
    mininet.vm.provider :virtualbox do |vb|
      vb.memory = 2048
    end
    mininet.vm.provider "vmware_fusion" do |vf|
      vf.vmx["memsize"] = "2048"
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
    dsctl.vm.provider "vmware_fusion" do |v, override|
      override.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/vmware/opscode_ubuntu-13.10_chef-provisionerless.box"
    end
    dsctl.vm.hostname = "devstack-control"
    dsctl.vm.network "private_network", ip: "192.168.50.20"
    dsctl.vm.network "forwarded_port", guest: 8080, host: 8081
    dsctl.vm.provider :virtualbox do |vb|
      vb.memory = 4096
    end
    dsctl.vm.provider "vmware_fusion" do |vf|
      vf.vmx["memsize"] = "4096"
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
    dscom.vm.provider "vmware_fusion" do |v, override|
      override.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/vmware/opscode_ubuntu-13.10_chef-provisionerless.box"
    end
    dscom.vm.hostname = "devstack-compute"
    dscom.vm.network "private_network", ip: "192.168.50.21"
    dscom.vm.provider :virtualbox do |vb|
      vb.memory = 4096
    end
    dscom.vm.provider "vmware_fusion" do |vf|
      vf.vmx["memsize"] = "4096"
    end
    dscom.vm.provision "puppet" do |puppet|
      puppet.hiera_config_path = "resources/puppet/hiera.yaml"
      puppet.working_directory = "/vagrant/resources/puppet"
      puppet.manifests_path = "resources/puppet/manifests"
      puppet.manifest_file  = "devstack-compute.pp"
    end
  end

end
