#Allow sudo access with no password
echo "vagrant ALL=NOPASSWD: ALL" >>/etc/sudoers

#disable reverse IP resolution on the VM
echo "UseDNS no" >>/etc/ssh/sshd_config

echo "Provisioning complete"
