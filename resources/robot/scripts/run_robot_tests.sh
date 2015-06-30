# clear OVS setup
sh /vagrant/scripts/clear_ovs.sh

# down the integration codes
if [ -d integration/test/csit/suites ]; then
    echo "refreshing test scripts"
    cd integration
    sudo git pull
    cd ~
else
   echo "downloading integration..."
   sudo git clone https://git.opendaylight.org/gerrit/integration
fi

# show the environmental variables
printf "CONTROLLER IP:%s\n" "$CONTROLLER"
printf "MININET VM IP:%s\n" "$MININET0"
printf "MININETII VM IP:%s\n" "$MININET1"

export test_suite_dir="$HOME/integration/test/csit/suites/ovsdb/"

echo "Waiting for controller to come up..."
COUNT="0"
while true; do
    RESP=`curl --user admin:admin -sL -w "%{http_code} %{url_effective}\\n" http://$CONTROLLER:8181/restconf/modules -o /dev/null`
    echo $RESP
    if [[ $RESP == *"200"* ]]; then
        echo Controller is UP
        break
    elif (( "$COUNT" > "600" )); then
        echo Timeout Controller DOWN
        exit 1
    else
        COUNT=$(( ${COUNT} + 5 ))
        sleep 5
        echo waiting $COUNT secs...
    fi
done

echo "Cool down for 1 min :)..."
sleep 60
sudo pybot -v CONTROLLER:$CONTROLLER -v MININET:$MININET0 -v MININET1:$MININET1 -v USER_HOME:$HOME -v MININET_USER:$USER $test_suite_dir

# export the results
if [ ! -d /vagrant/scripts/results ]; then
    echo "creating results folder"
    mkdir /vagrant/scripts/results
fi

# move test output to the shared results folder
#  -- output.xml, log.html and report.html generated 		
#     after each run is saved in a shared timestamp folder 		
#     under /vagrant/scripts/results 

timestamp=$(date +'%Y.%m.%d-%H.%M.%S')
mkdir /vagrant/scripts/results/$timestamp
cp $PWD/output.xml /vagrant/scripts/results/$timestamp/output.xml
cp $PWD/log.html /vagrant/scripts/results/$timestamp/log.html
cp $PWD/report.html /vagrant/scripts/results/$timestamp/report.html




