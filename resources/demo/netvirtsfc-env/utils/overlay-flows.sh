#!/usr/bin/env bash
# Add flows for the normal overlay that Netvirt would have added
# sw1: h35_2, dl_src=00:00:00:00:35:02
# sw6: h35_4, dl_src=00:00:00:00:35:04

set -e
hostnum=${HOSTNAME#"netvirtsfc"}
sw="sw$hostnum"

if [ "$hostnum" -eq "1" ]; then
    # ARP responder for h35_4
    sudo ovs-ofctl -O OpenFlow13 add-flow $sw "table=0,arp,arp_tpa=10.0.35.4,arp_op=1 actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:00:00:00:00:35:04->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],load:0x000000003504->NXM_NX_ARP_SHA[],move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],load:0x0a002304->NXM_OF_ARP_SPA[],IN_PORT"

    #port=$(ip -o link | grep veth | awk '{print$2}' | sed 's/://')
    # l2 forward of local traffic to the normal vxlan
    sudo ovs-ofctl -O OpenFlow13 add-flow $sw "table=0,priority=150,in_port=1,dl_src=00:00:00:00:35:02,dl_dst=00:00:00:00:35:04,actions=output:5"

    # l2 forward of incoming vxlan traffic to the local port
    sudo ovs-ofctl -O OpenFlow13 add-flow $sw "table=0,priority=150,in_port=5,dl_src=00:00:00:00:35:04,dl_dst=00:00:00:00:35:02,actions=output:1"

elif [ "$hostnum" -eq "6" ]; then
    # ARP responder for h35_4
    sudo ovs-ofctl -O OpenFlow13 add-flow $sw "table=0,arp,arp_tpa=10.0.35.2,arp_op=1 actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:00:00:00:00:35:02->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],load:0x000000003502->NXM_NX_ARP_SHA[],move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],load:0x0a002302->NXM_OF_ARP_SPA[],IN_PORT"

    # l2 forward of local traffic to the normal vxlan
    sudo ovs-ofctl -O OpenFlow13 add-flow $sw "table=0,priority=150,in_port=1,dl_src=00:00:00:00:35:04,dl_dst=00:00:00:00:35:02,actions=output:5"

    # l2 forward of incoming vxlan traffic to the local port
    sudo ovs-ofctl -O OpenFlow13 add-flow $sw "table=0,priority=150,in_port=5,dl_src=00:00:00:00:35:02,dl_dst=00:00:00:00:35:04,actions=output:1"

else
    echo "Invalid SF for this demo";
    exit
fi
