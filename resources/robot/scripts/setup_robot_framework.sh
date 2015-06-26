#ensure that CentOS5/EL5 repo is setup
cd /tmp
sudo wget -r --no-parent -A 'epel-release-*.rpm' http://dl.fedoraproject.org/pub/epel/7/x86_64/e/ 
sudo rpm -Uvh dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-*.rpm

#install git
sudo yum install -y git

# python is already installed.
# install python-pip and python-paramiko
sudo yum install -y python-pip
sudo yum install -y python-paramiko

#install the robot framework and required libraries
sudo pip install requests
sudo pip install robotframework
sudo pip install robotframework-sshlibrary
sudo pip install -U robotframework-requests
sudo pip install --upgrade robotframework-httplibrary

#copy the robot trigger code to user home
cp /vagrant/scripts/run_robot_tests.sh ~/
