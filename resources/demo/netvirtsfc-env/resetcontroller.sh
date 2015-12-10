hostnum=${HOSTNAME#"netvirtsfc"}
sw="sw$hostnum"
echo "Deleting controller for $sw"
ovs-vsctl del-controller $sw;
if [[ $? -ne 0 ]] ; then
    exit 1
fi
echo "Sleeping for 6sec..."
sleep 6
echo "Setting controller to $ODL"
ovs-vsctl set-controller $sw tcp:$ODL:6653
