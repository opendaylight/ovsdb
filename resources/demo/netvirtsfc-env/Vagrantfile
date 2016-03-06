
# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  odl=ENV['ODL']
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "512"
  end

  # run our bootstrapping for the system
  config.vm.provision 'shell', path: 'bootstrap.sh', :args => odl

  num_nodes = (ENV['NUM_NODES'] || 1).to_i

  # ip configuration
  ip_base = (ENV['SUBNET'] || "192.168.50.")
  ips = num_nodes.times.collect { |n| ip_base + "#{n+70}" }

  num_nodes.times do |n|
    config.vm.define "netvirtsfc#{n+1}", autostart: true do |compute|
      vm_ip = ips[n]
      vm_index = n+1
      compute.vm.box = "ubuntu/trusty64"
      compute.vm.hostname = "netvirtsfc#{vm_index}"
      compute.vm.network "private_network", ip: "#{vm_ip}"
      compute.vm.provider :virtualbox do |vb|
        vb.memory = 512
        vb.customize ["modifyvm", :id, "--ioapic", "on"]      
        vb.cpus = 1
      end
    end
  end
end
