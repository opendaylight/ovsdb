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

def rest_delete(host, port, uri, debug=False):
    '''Perform a DELETE rest operation, using the URL and data provided'''
    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "DELETE %s" % url
    try:
        r = requests.delete(url, headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    except (requests.exceptions.ConnectionError, requests.exceptions.HTTPError) as e:
        print "oops: ", e
        return
    if debug == True:
        print r.text
    try:
        r.raise_for_status()
    except:
        print "oops: ", sys.exc_info()[0]


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

def get_service_function_forwarders_uri():
    return "/restconf/config/service-function-forwarder:service-function-forwarders"

def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_tenant_uri():
    return "/restconf/config/policy:tenants/policy:tenant/f5c7d344-d1c7-4208-8531-2c2693657e12"

def get_tunnel_uri():
    return "/restconf/config/opendaylight-inventory:nodes"

def get_endpoint_uri():
    return "/restconf/operations/endpoint:unregister-endpoint"

def get_ietf_acl_uri():
    return "/restconf/config/ietf-access-control-list:access-lists"

def get_classifier_uri():
    return "/restconf/config/netvirt-sfc-classifier:classifiers"

def get_netvirt_sfc_uri():
    return "/restconf/config/netvirt-sfc:sfc/"

if __name__ == "__main__":
    # Launch main menu


    # Some sensible defaults
    controller=os.environ.get('ODL')
    if controller == None:
        sys.exit("No controller set.")
    else:
	print "Contacting controller at %s" % controller

    #resp=get(controller,DEFAULT_PORT,'/restconf/operational/endpoint:endpoints')
    #l2_eps=resp['endpoints']['endpoint']
    #l3_eps=resp['endpoints']['endpoint-l3']

    print "deleting service function paths"
    rest_delete(controller, DEFAULT_PORT, get_service_function_paths_uri(), True)

    print "deleting service function chains"
    rest_delete(controller, DEFAULT_PORT, get_service_function_chains_uri(), True)

    print "deleting service functions"
    rest_delete(controller, DEFAULT_PORT, get_service_functions_uri(), True)

    print "deleting service function forwarders"
    rest_delete(controller, DEFAULT_PORT, get_service_function_forwarders_uri(), True)

    #print "deleting tunnel"
    #rest_delete(controller, DEFAULT_PORT, get_tunnel_uri(), True)

    #print "deleting tenant"
    #rest_delete(controller, DEFAULT_PORT, get_tenant_uri(), True)

    #print "unregistering L2 endpoints"
    #for endpoint in l2_eps:
    #data={ "input": { "l2": [ { "l2-context": endpoint['l2-context'] ,"mac-address": endpoint['mac-address'] } ] } }
    #    post(controller, DEFAULT_PORT, get_endpoint_uri(),data,True)

    #print "unregistering L3 endpoints"
    #for endpoint in l3_eps:
    #data={ "input": { "l3": [ { "l3-context": endpoint['l3-context'] ,"ip-address": endpoint['ip-address'] } ] } }
    #    post(controller, DEFAULT_PORT, get_endpoint_uri(),data,True)

    print "deleting acl"
    rest_delete(controller, DEFAULT_PORT, get_ietf_acl_uri(), True)

    print "deleting classifier"
    rest_delete(controller, DEFAULT_PORT, get_classifier_uri(), True)

    print "deleting netvirt sfc"
    rest_delete(controller, DEFAULT_PORT, get_netvirt_sfc_uri(), True)
