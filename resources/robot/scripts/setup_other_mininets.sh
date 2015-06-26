# add the ssh to authorized keys
cat /vagrant/shared/id_rsa.pub >> ~/.ssh/authorized_keys
sudo chmod 600 ~/.ssh/authorized_keys

#Modify the user prompt termination from "$" to ">"
echo "PS1='\u@\h:\w\> '" >> ~/.bashrc
source ~/.bashrc 

