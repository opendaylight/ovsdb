# create the ssh key
if [ ! -f ~/.ssh/id_rsa ]; then
    echo "creating ssh key..."
    ssh-keygen -t rsa -N "" -f ~/.ssh/id_rsa
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
    sudo chmod 600 ~/.ssh/authorized_keys
fi

# copy this key to vagrant folder
cp ~/.ssh/id_rsa.pub /vagrant/shared

#Modify the user prompt termination from "$" to ">"
echo "PS1='\u@\h:\w\> '" >> ~/.bashrc
source ~/.bashrc
