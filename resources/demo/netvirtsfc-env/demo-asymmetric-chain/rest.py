#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os


DEFAULT_PORT='8181'


USERNAME='admin'
PASSWORD='admin'


OPER_NODES='/restconf/operational/opendaylight-inventory:nodes/'
CONF_TENANT='/restconf/config/policy:tenants'

def get(host, port, uri):
    url='http://'+host+":"+port+uri
    #print url
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    jsondata=json.loads(r.text)
    return jsondata

def put(host, port, uri, data, debug=False):
    '''Perform a PUT rest operation, using the URL and data provided'''

    url='http://'+host+":"+port+uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "PUT %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()

def post(host, port, uri, data, debug=False):
    '''Perform a POST rest operation, using the URL and data provided'''

    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "POST %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()

def get_service_functions_uri():
    return "/restconf/config/service-function:service-functions"

def get_service_functions_data():
    return {
    "service-functions": {
        "service-function": [
            {
                "name": "firewall-72",
                "ip-mgmt-address": "192.168.50.72",
                "type": "service-function-type:firewall",
                "nsh-aware": "true",
                "sf-data-plane-locator": [
                    {
                        "name": "sf1Dpl",
                        "port": 6633,
                        "ip": "192.168.50.72",
                        "transport": "service-locator:vxlan-gpe",
                        "service-function-forwarder": "SFF1"
                    }
                ]
            },
            {
                "name": "dpi-74",
                "ip-mgmt-address": "192.168.50.74",
                "type": "service-function-type:dpi",
                "nsh-aware": "true",
                "sf-data-plane-locator": [
                    {
                        "name": "sf2Dpl",
                        "port": 6633,
                        "ip": "192.168.50.74",
                        "transport": "service-locator:vxlan-gpe",
                        "service-function-forwarder": "SFF2"
                    }
                ]
            }
        ]
    }
}

def get_service_function_forwarders_uri():
    return "/restconf/config/service-function-forwarder:service-function-forwarders"

def get_service_function_forwarders_data():
    return {
    "service-function-forwarders": {
        "service-function-forwarder": [
            {
                "name": "SFF1",
                "service-node": "OVSDB2",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "sw2"
                },
                "service-function-dictionary": [
                    {
                        "name": "firewall-72",
                        "sff-sf-data-plane-locator": {
                            "sff-dpl-name": "sfc-tun2",
                            "sf-dpl-name": "sf1Dpl"
                        }
                    }
                ],
                "sff-data-plane-locator": [
                    {
                        "name": "sfc-tun2",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 6633,
                            "ip": "192.168.50.71"
                        },
                        "service-function-forwarder-ovs:ovs-options": {
                            "remote-ip": "flow",
                            "dst-port": "6633",
                            "key": "flow",
                            "nsp": "flow",
                            "nsi": "flow",
                            "nshc1": "flow",
                            "nshc2": "flow",
                            "nshc3": "flow",
                            "nshc4": "flow"
                        }
                    }
                ]
            },
            {
                "name": "SFF2",
                "service-node": "OVSDB2",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "sw4"
                },
                "service-function-dictionary": [
                    {
                        "name": "dpi-74",
                        "sff-sf-data-plane-locator": {
                            "sff-dpl-name": "sfc-tun4",
                            "sf-dpl-name": "sf2Dpl"
                        }
                    }
                ],
                "sff-data-plane-locator": [
                    {
                        "name": "sfc-tun4",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 6633,
                            "ip": "192.168.50.73"
                        },
                        "service-function-forwarder-ovs:ovs-options": {
                            "remote-ip": "flow",
                            "dst-port": "6633",
                            "key": "flow",
                            "nsp": "flow",
                            "nsi": "flow",
                            "nshc1": "flow",
                            "nshc2": "flow",
                            "nshc3": "flow",
                            "nshc4": "flow"
                        }
                    }
                ]
            }
        ]
    }
}

def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_service_function_chains_data():
    return {
    "service-function-chains": {
        "service-function-chain": [
            {
                "name": "SFCNETVIRT",
                "symmetric": "false",
                "sfc-service-function": [
                    {
                        "name": "firewall-abstract1",
                        "type": "service-function-type:firewall"
                    },
                    {
                        "name": "dpi-abstract1",
                        "type": "service-function-type:dpi"
                    }
                ]
            }
        ]
    }
}

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_service_function_paths_data():
    return {
    "service-function-paths": {
        "service-function-path": [
            {
                "name": "SFCNETVIRT-Path",
                "service-chain-name": "SFCNETVIRT",
                "starting-index": 255,
                "symmetric": "false"

            }
        ]
    }
}

def get_ietf_acl_uri():
    return "/restconf/config/ietf-access-control-list:access-lists"

def get_ietf_acl_data():
    return {
        "access-lists": {
            "acl": [
                {
                    "acl-name": "http-acl",
                    "access-list-entries": {
                        "ace": [
                            {
                                "rule-name": "http-rule",
                                "matches": {
                                    "protocol": "6",
                                    "destination-port-range": {
                                        "lower-port": "80",
                                        "upper-port": "80"
                                    },
                                },
                                "actions": {
                                    "netvirt-sfc-acl:sfc-name": "SFCNETVIRT"
                                }
                            }
                        ]
                    }
                }
            ]
        }
    }

def get_classifier_uri():
    return "/restconf/config/netvirt-sfc-classifier:classifiers"

def get_classifier_data():
    return {
        "classifiers": {
            "classifier": [
                {
                    "name": "http-classifier",
                    "acl": "http-acl",
                    "sffs": {
                        "sff": [
                            {
                                "name": "SFF1"
                            }
                        ]
                    },
                    "bridges": {
                        "bridge": [
                            {
                                "name": "sw1",
                                "direction": "ingress"
                            },
                            {
                                "name": "sw6",
                                "direction": "egress"
                            }
                        ]
                    }
                }
            ]
        }
    }

def get_netvirt_sfc_uri():
    return "/restconf/config/netvirt-sfc:sfc/"

def get_netvirt_sfc_data():
    return {
        "sfc": {
            "name": "sfc1"
        }
    }

if __name__ == "__main__":
    # Launch main menu


    # Some sensible defaults
    controller=os.environ.get('ODL')
    if controller == None:
        sys.exit("No controller set.")
    else:
	print "Contacting controller at %s" % controller

    #tenants=get(controller,DEFAULT_PORT,CONF_TENANT)

    print "sending service functions"
    put(controller, DEFAULT_PORT, get_service_functions_uri(), get_service_functions_data(), True)
    print "sending service function forwarders"
    put(controller, DEFAULT_PORT, get_service_function_forwarders_uri(), get_service_function_forwarders_data(), True)

    print "sf's and sff's created"
    time.sleep(5)
    print "sending service function chains"
    put(controller, DEFAULT_PORT, get_service_function_chains_uri(), get_service_function_chains_data(), True)
    print "sending service function paths"
    put(controller, DEFAULT_PORT, get_service_function_paths_uri(), get_service_function_paths_data(), True)

    print "sfc's and sfp's created"
    time.sleep(5)
    print "sending netvirt-sfc"
    put(controller, DEFAULT_PORT, get_netvirt_sfc_uri(), get_netvirt_sfc_data(), True)
    time.sleep(1)
    print "sending ietf-acl"
    put(controller, DEFAULT_PORT, get_ietf_acl_uri(), get_ietf_acl_data(), True)
    time.sleep(1)
    print "sending classifier"
    put(controller, DEFAULT_PORT, get_classifier_uri(), get_classifier_data(), True)


    # print "sending tunnel -- SKIPPED"
    ## put(controller, DEFAULT_PORT, get_tunnel_uri(), get_tunnel_data(), True)
    # print "sending tenant -- SKIPPED"
    ## put(controller, DEFAULT_PORT, get_tenant_uri(), get_tenant_data(),True)
    # print "registering endpoints -- SKIPPED"
    ## for endpoint in get_endpoint_data():
    ##    post(controller, DEFAULT_PORT, get_endpoint_uri(),endpoint,True)


