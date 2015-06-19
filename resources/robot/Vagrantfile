# -*- mode: ruby -*-
# # vi: set ft=ruby :

# Specify minimum Vagrant version and Vagrant API version
Vagrant.require_version ">= 1.6.0"
VAGRANTFILE_API_VERSION = "2"

# Create boxes
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

    # Get the number of OVS VMs to create
    num_ovs_vms = (ENV['OVS_NODES'] || 2).to_i

    base_name = "ovs"
    base_ip = "192.168.100."
    controller_ip = "192.168.100.10"
    os = "puppetlabs/centos-7.0-64-puppet"
    ram = "512"
    ovsversion = "2.3.1"
    ovs_node_ips = num_ovs_vms.times.collect { |n| base_ip + "#{n+21}" }

    num_ovs_vms.times do |n|
    config.vm.define base_name + "#{n+1}", autostart: true do |srv|
          ## Extract yaml variables
          hostname = base_name + "#{n+1}"
          local_ip = ovs_node_ips[n]

              srv.vm.hostname = hostname
          srv.vm.box = os
          srv.vm.network "private_network", ip: local_ip
          srv.vm.provider :virtualbox do |vb|
        vb.name = hostname
        vb.memory = ram
          end # vb

          # Set guest environment variables
          command1 = 'export CONTROLLER=\"' + controller_ip + '\"'
          command2 = 'export LOCAL_IP=\"' + local_ip + '\"'
          srv.vm.provision :shell, privileged: true, inline: 'echo ' + command1 + ' >> /etc/profile'
          srv.vm.provision :shell, privileged: true, inline: 'echo ' + command2 + ' >> /etc/profile'
          srv.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"

          ## SSH config
          srv.ssh.forward_x11 = false

          ## puppet
          srv.vm.provision "puppet" do |puppet|
        puppet.working_directory = "/vagrant/puppet"
        puppet.manifests_path = "puppet/manifests"
        puppet.manifest_file  = "ovsnode.pp"
        puppet.options = "--verbose --debug"
        puppet.facter = {
          "ovsversion" => ovsversion,
        }
          end # puppet


          #process setup commands for ovs
          srv.vm.provision "shell", path: "puppet/scripts/setup_defaults.sh"

        end # srv
    end #end num_ovs_vms
end # config
