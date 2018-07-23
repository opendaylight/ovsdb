OVSDB Developer Guide
=====================

OVSDB Integration
-----------------

The Open vSwitch database (OVSDB) Southbound Plugin component for
OpenDaylight implements the OVSDB `RFC
7047 <https://tools.ietf.org/html/rfc7047>`__ management protocol that
allows the southbound configuration of switches that support OVSDB. The
component comprises a library and a plugin. The OVSDB protocol uses
JSON-RPC calls to manipulate a physical or virtual switch that supports
OVSDB. Many vendors support OVSDB on various hardware platforms. The
OpenDaylight controller uses the library project to interact with an OVS
instance.

.. note::

    Read the OVSDB User Guide before you begin development.

OpenDaylight OVSDB southbound plugin architecture and design
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

OpenVSwitch (OVS) is generally accepted as the unofficial standard for
Virtual Switching in the Open hypervisor based solutions. Every other
Virtual Switch implementation, properietery or otherwise, uses OVS in
some form. For information on OVS, see `Open
vSwitch <http://openvswitch.org/>`__.

In Software Defined Networking (SDN), controllers and applications
interact using two channels: OpenFlow and OVSDB. OpenFlow addresses the
forwarding-side of the OVS functionality. OVSDB, on the other hand,
addresses the management-plane. A simple and concise overview of Open
Virtual Switch Database(OVSDB) is available at:
http://networkstatic.net/getting-started-ovsdb/

Overview of OpenDaylight Controller architecture
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The OpenDaylight controller platform is designed as a highly modular and
plugin based middleware that serves various network applications in a
variety of use-cases. The modularity is achieved through the Java OSGi
framework. The controller consists of many Java OSGi bundles that work
together to provide the required controller functionalities.

| The bundles can be placed in the following broad categories:

-  Network Service Functional Modules (Examples: Topology Manager,
   Inventory Manager, Forwarding Rules Manager,and others)

-  NorthBound API Modules (Examples: Topology APIs, Bridge Domain APIs,
   Neutron APIs, Connection Manager APIs, and others)

-  Service Abstraction Layer(SAL)- (Inventory Services, DataPath
   Services, Topology Services, Network Config, and others)

-  SouthBound Plugins (OpenFlow Plugin, OVSDB Plugin, OpenDove Plugin,
   and others)

-  Application Modules (Simple Forwarding, Load Balancer)

Each layer of the Controller architecture performs specified tasks, and
hence aids in modularity. While the Northbound API layer addresses all
the REST-Based application needs, the SAL layer takes care of
abstracting the SouthBound plugin protocol specifics from the Network
Service functions.

Each of the SouthBound Plugins serves a different purpose, with some
overlapping. For example, the OpenFlow plugin might serve the Data-Plane
needs of an OVS element, while the OVSDB plugin can serve the management
plane needs of the same OVS element. As the OpenFlow Plugin talks
OpenFlow protocol with the OVS element, the OVSDB plugin will use OVSDB
schema over JSON-RPC transport.

OVSDB southbound plugin
~~~~~~~~~~~~~~~~~~~~~~~

| The `Open vSwitch Database Management
  Protocol-draft-02 <http://tools.ietf.org/html/draft-pfaff-ovsdb-proto-02>`__
  and `Open vSwitch
  Manual <http://openvswitch.org/ovs-vswitchd.conf.db.5.pdf>`__ provide
  theoretical information about OVSDB. The OVSDB protocol draft is
  generic enough to lay the groundwork on Wire Protocol and Database
  Operations, and the OVS Manual currently covers 13 tables leaving
  space for future OVS expansion, and vendor expansions on proprietary
  implementations. The OVSDB Protocol is a database records transport
  protocol using JSON RPC1.0. For information on the protocol structure,
  see `Getting Started with
  OVSDB <http://networkstatic.net/getting-started-ovsdb/>`__. The
  OpenDaylight OVSDB southbound plugin consists of one or more OSGi
  bundles addressing the following services or functionalities:

-  Connection Service - Based on Netty

-  Network Configuration Service

-  Bidirectional JSON-RPC Library

-  OVSDB Schema definitions and Object mappers

-  Overlay Tunnel management

-  OVSDB to OpenFlow plugin mapping service

-  Inventory Service

Connection service
~~~~~~~~~~~~~~~~~~

| One of the primary services that most southbound plugins provide in
  OpenDaylight a Connection Service. The service provides protocol
  specific connectivity to network elements, and supports the
  connectivity management services as specified by the OpenDaylight
  Connection Manager. The connectivity services include:

-  Connection to a specified element given IP-address, L4-port, and
   other connectivity options (such as authentication,…)

-  Disconnection from an element

-  Handling Cluster Mode change notifications to support the
   OpenDaylight Clustering/High-Availability feature

Network Configuration Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

| The goal of the OpenDaylight Network Configuration services is to
  provide complete management plane solutions needed to successfully
  install, configure, and deploy the various SDN based network services.
  These are generic services which can be implemented in part or full by
  any south-bound protocol plugin. The south-bound plugins can be either
  of the following:

-  The new network virtualization protocol plugins such as OVSDB
   JSON-RPC

-  The traditional management protocols such as SNMP or any others in
   the middle.

The above definition, and more information on Network Configuration
Services, is available at :
https://wiki.opendaylight.org/view/OpenDaylight_Controller:NetworkConfigurationServices

Bidirectional JSON-RPC library
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The OVSDB plugin implements a Bidirectional JSON-RPC library. It is easy
to design the library as a module that manages the Netty connection
towards the Element.

| The main responsibilities of this Library are:

-  Demarshal and marshal JSON Strings to JSON objects

-  Demarshal and marshal JSON Strings from and to the Network Element.

OVSDB Schema definitions and Object mappers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The OVSDB Schema definitions and Object Mapping layer sits above the
JSON-RPC library. It maps the generic JSON objects to OVSDB schema POJOs
(Plain Old Java Object) and vice-versa. This layer mostly provides the
Java Object definition for the corresponding OVSDB schema (13 of them)
and also will provide much more friendly API abstractions on top of
these object data. This helps in hiding the JSON semantics from the
functional modules such as Configuration Service and Tunnel management.

| On the demarshaling side the mapping logic differentiates the Request
  and Response messages as follows :

-  Request messages are mapped by its "method"

-  | Response messages are mapped by their IDs which were originally
     populated by the Request message. The JSON semantics of these OVSDB
     schema is quite complex. The following figures summarize two of the
     end-to-end scenarios:

.. figure:: ./images/ConfigurationService-example1.png
   :alt: End-to-end handling of a Create Bridge request

   End-to-end handling of a Create Bridge request

.. figure:: ./images/MonitorResponse.png
   :alt: End-to-end handling of a monitor response

   End-to-end handling of a monitor response

Overlay tunnel management
^^^^^^^^^^^^^^^^^^^^^^^^^

Network Virtualization using OVS is achieved through Overlay Tunnels.
The actual Type of the Tunnel may be GRE, VXLAN, or STT. The differences
in the encapsulation and configuration decide the tunnel types.
Establishing a tunnel using configuration service requires just the
sending of OVSDB messages towards the ovsdb-server. However, the scaling
issues that would arise on the state management at the data-plane (using
OpenFlow) can get challenging. Also, this module can assist in various
optimizations in the presence of Gateways. It can also help in providing
Service guarantees for the VMs using these overlays with the help of
underlay orchestration.

OVSDB to OpenFlow plugin mapping service
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

| The connect() of the ConnectionService would result in a Node that
  represents an ovsdb-server. The CreateBridgeDomain() Configuration on
  the above Node would result in creating an OVS bridge. This OVS Bridge
  is an OpenFlow Agent for the OpenDaylight OpenFlow plugin with its own
  Node represented as (example) OF\|xxxx.yyyy.zzzz. Without any help
  from the OVSDB plugin, the Node Mapping Service of the Controller
  platform would not be able to map the following:

::

    {OVSDB_NODE + BRIDGE_IDENTFIER} <---> {OF_NODE}.

Without such mapping, it would be extremely difficult for the
applications to manage and maintain such nodes. This Mapping Service
provided by the OVSDB plugin would essentially help in providing more
value added services to the orchestration layers that sit atop the
Northbound APIs (such as OpenStack).

OVSDB: New features
~~~~~~~~~~~~~~~~~~~

Schema independent library
^^^^^^^^^^^^^^^^^^^^^^^^^^

The OVS connection is a node which can have multiple databases. Each
database is represented by a schema. A single connection can have
multiple schemas. OSVDB supports multiple schemas. Currently, these are
two schemas available in the OVSDB, but there is no restriction on the
number of schemas. Owing to the Northbound v3 API, no code changes in
ODL are needed for supporting additional schemas.

| Schemas:

-  openvswitch : Schema wrapper that represents
   http://openvswitch.org/ovs-vswitchd.conf.db.5.pdf

-  hardwarevtep: Schema wrapper that represents
   http://openvswitch.org/docs/vtep.5.pdf

OVSDB Library Developer Guide
-----------------------------

Overview
~~~~~~~~

The OVSDB library manages the Netty connections to network nodes and
handles bidirectional JSON-RPC messages. It not only provides OVSDB
protocol functionality to OpenDaylight OVSDB plugin but also can be used
as standalone JAVA library for OVSDB protocol.

The main responsibilities of OVSDB library include:

-  Manage connections to peers

-  Marshal and unmarshal JSON Strings to JSON objects.

-  Marshal and unmarshal JSON Strings from and to the Network Element.

Connection Service
~~~~~~~~~~~~~~~~~~

The OVSDB library provides connection management through the
OvsdbConnection interface. The OvsdbConnection interface provides OVSDB
connection management APIs which include both active and passive
connections. From the library perspective, active OVSDB connections are
initiated from the controller to OVS nodes while passive OVSDB
connections are initiated from OVS nodes to the controller. In the
active connection scenario an application needs to provide the IP
address and listening port of OVS nodes to the library management API.
On the other hand, the library management API only requires the info of
the controller listening port in the passive connection scenario.

For a passive connection scenario, the library also provides a
connection event listener through the OvsdbConnectionListener interface.
The listener interface has connected() and disconnected() methods to
notify an application when a new passive connection is established or an
existing connection is terminated.

SSL Connection
~~~~~~~~~~~~~~

In addition to a regular TCP connection, the OvsdbConnection interface
also provides a connection management API for an SSL connection. To
start an OVSDB connection with SSL, an application will need to provide
a Java SSLContext object to the management API. There are different ways
to create a Java SSLContext, but in most cases a Java KeyStore with
certificate and private key provided by the application is required.
Detailed steps about how to create a Java SSLContext is out of the scope
of this document and can be found in the Java documentation for `JAVA
Class SSlContext <http://goo.gl/5svszT>`__.

In the active connection scenario, the library uses the given SSLContext
to create a Java SSLEngine and configures the SSL engine with the client
mode for SSL handshaking. Normally clients are not required to
authenticate themselves.

In the passive connection scenario, the library uses the given
SSLContext to create a Java SSLEngine which will operate in server mode
for SSL handshaking. For security reasons, the SSLv3 protocol and some
cipher suites are disabled. Currently the OVSDB server only supports the
TLS\_RSA\_WITH\_AES\_128\_CBC\_SHA cipher suite and the following
protocols: SSLv2Hello, TLSv1, TLSv1.1, TLSv1.2.

The SSL engine is also configured to operate on two-way authentication
mode for passive connection scenarios, i.e., the OVSDB server
(controller) will authenticate clients (OVS nodes) and clients (OVS
nodes) are also required to authenticate the server (controller). In the
two-way authentication mode, an application should keep a trust manager
to store the certificates of trusted clients and initialize a Java
SSLContext with this trust manager. Thus during the SSL handshaking
process the OVSDB server (controller) can use the trust manager to
verify clients and only accept connection requests from trusted clients.
On the other hand, users should also configure OVS nodes to authenticate
the controller. Open vSwitch already supports this functionality in the
ovsdb-server command with option ``--ca-cert=cacert.pem`` and
``--bootstrap-ca-cert=cacert.pem``. On the OVS node, a user can use the
option ``--ca-cert=cacert.pem`` to specify a controller certificate
directly and the node will only allow connections to the controller with
the specified certificate. If the OVS node runs ovsdb-server with option
``--bootstrap-ca-cert=cacert.pem``, it will authenticate the controller
with the specified certificate cacert.pem. If the certificate file
doesn’t exist, it will attempt to obtain a certificate from the peer
(controller) on its first SSL connection and save it to the named PEM
file ``cacert.pem``. Here is an example of ovsdb-server with
``--bootstrap-ca-cert=cacert.pem`` option:

``ovsdb-server --pidfile --detach --log-file --remote punix:/var/run/openvswitch/db.sock --remote=db:hardware_vtep,Global,managers --private-key=/etc/openvswitch/ovsclient-privkey.pem -- certificate=/etc/openvswitch/ovsclient-cert.pem --bootstrap-ca-cert=/etc/openvswitch/vswitchd.cacert``

OVSDB protocol transactions
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The OVSDB protocol defines the RPC transaction methods in RFC 7047. The
following RPC methods are supported in OVSDB protocol:

-  List databases

-  Get schema

-  Transact

-  Cancel

-  Monitor

-  Update notification

-  Monitor cancellation

-  Lock operations

-  Locked notification

-  Stolen notification

-  Echo

According to RFC 7047, an OVSDB server must implement all methods, and
an OVSDB client is only required to implement the "Echo" method and
otherwise free to implement whichever methods suit its needs. However,
the OVSDB library currently doesn’t support all RPC methods. For the
"Echo" method, the library can handle "Echo" messages from a peer and
send a JSON response message back, but the library doesn’t support
actively sending an "Echo" JSON request to a peer. Other unsupported RPC
methods are listed below:

-  Cancel

-  Lock operations

-  Locked notification

-  Stolen notification

In the OVSDB library the RPC methods are defined in the Java interface
OvsdbRPC. The library also provides a high-level interface OvsdbClient
as the main interface to interact with peers through the OVSDB protocol.
In the passive connection scenario, each connection will have a
corresponding OvsdbClient object, and the application can obtain the
OvsdbClient object through connection listener callback methods. In
other words, if the application implements the OvsdbConnectionListener
interface, it will get notifications of connection status changes with
the corresponding OvsdbClient object of that connection.

OVSDB database operations
~~~~~~~~~~~~~~~~~~~~~~~~~

RFC 7047 also defines database operations, such as insert, delete, and
update, to be performed as part of a "transact" RPC request. The OVSDB
library defines the data operations in Operations.java and provides the
TransactionBuilder class to help build "transact" RPC requests. To build
a JSON-RPC transact request message, the application can obtain the
TransactionBuilder object through a transactBuilder() method in the
OvsdbClient interface.

The TransactionBuilder class provides the following methods to help
build transactions:

-  getOperations(): Get the list of operations in this transaction.

-  add(): Add data operation to this transaction.

-  build(): Return the list of operations in this transaction. This is
   the same as the getOperations() method.

-  execute(): Send the JSON RPC transaction to peer.

-  getDatabaseSchema(): Get the database schema of this transaction.

If the application wants to build and send a "transact" RPC request to
modify OVSDB tables on a peer, it can take the following steps:

1. Statically import parameter "op" in Operations.java

   ``import static org.opendaylight.ovsdb.lib.operations.Operations.op;``

2. Obtain transaction builder through transacBuilder() method in
   OvsdbClient:

   ``TransactionBuilder transactionBuilder = ovsdbClient.transactionBuilder(dbSchema);``

3. Add operations to transaction builder:

   ``transactionBuilder.add(op.insert(schema, row));``

4. Send transaction to peer and get JSON RPC response:

   ``operationResults = transactionBuilder.execute().get();``

   .. note::

       Although the "select" operation is supported in the OVSDB
       library, the library implementation is a little different from
       RFC 7047. In RFC 7047, section 5.2.2 describes the "select"
       operation as follows:

   “The "rows" member of the result is an array of objects. Each object
   corresponds to a matching row, with each column specified in
   "columns" as a member, the column’s name as the member name, and its
   value as the member value. If "columns" is not specified, all the
   table’s columns are included (including the internally generated
   "\_uuid" and "\_version" columns).”

   The OVSDB library implementation always requires the column’s name in
   the "columns" field of a JSON message. If the "columns" field is not
   specified, none of the table’s columns are included. If the
   application wants to get the table entry with all columns, it needs
   to specify all the columns’ names in the "columns" field.

Reference Documentation
~~~~~~~~~~~~~~~~~~~~~~~

RFC 7047 The Open vSwitch Databse Management Protocol
https://tools.ietf.org/html/rfc7047

OVSDB MD-SAL Southbound Plugin Developer Guide
----------------------------------------------

Overview
~~~~~~~~

The Open vSwitch Database (OVSDB) Model Driven Service Abstraction Layer
(MD-SAL) Southbound Plugin provides an MD-SAL based interface to Open
vSwitch systems. This is done by augmenting the MD-SAL topology node
with a YANG model which replicates some (but not all) of the Open
vSwitch schema.

OVSDB MD-SAL Southbound Plugin Architecture and Operation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The architecture and operation of the OVSDB MD-SAL Southbound plugin is
illustrated in the following set of diagrams.

Connecting to an OVSDB Node
^^^^^^^^^^^^^^^^^^^^^^^^^^^

An OVSDB node is a system which is running the OVS software and is
capable of being managed by an OVSDB manager. The OVSDB MD-SAL
Southbound plugin in OpenDaylight is capable of operating as an OVSDB
manager. Depending on the configuration of the OVSDB node, the
connection of the OVSDB manager can be active or passive.

Active OVSDB Node Manager Workflow
''''''''''''''''''''''''''''''''''

An active OVSDB node manager connection is made when OpenDaylight
initiates the connection to the OVSDB node. In order for this to work,
you must configure the OVSDB node to listen on a TCP port for the
connection (i.e. OpenDaylight is active and the OVSDB node is passive).
This option can be configured on the OVSDB node using the following
command:

::

    ovs-vsctl set-manager ptcp:6640

The following diagram illustrates the sequence of events which occur
when OpenDaylight initiates an active OVSDB manager connection to an
OVSDB node.

.. figure:: ./images/ovsdb-sb-active-connection.jpg
   :alt: Active OVSDB Manager Connection

   Active OVSDB Manager Connection

Step 1
    Create an OVSDB node by using RESTCONF or an OpenDaylight plugin.
    The OVSDB node is listed under the OVSDB topology node.

Step 2
    Add the OVSDB node to the OVSDB MD-SAL southbound configuration
    datastore. The OVSDB southbound provider is registered to listen for
    data change events on the portion of the MD-SAL topology data store
    which contains the OVSDB southbound topology node augmentations. The
    addition of an OVSDB node causes an event which is received by the
    OVSDB Southbound provider.

Step 3
    The OVSDB Southbound provider initiates a connection to the OVSDB
    node using the connection information provided in the configuration
    OVSDB node (i.e. IP address and TCP port number).

Step 4
    The OVSDB Southbound provider adds the OVSDB node to the OVSDB
    MD-SAL operational data store. The operational data store contains
    OVSDB node objects which represent active connections to OVSDB
    nodes.

Step 5
    The OVSDB Southbound provider requests the schema and databases
    which are supported by the OVSDB node.

Step 6
    The OVSDB Southbound provider uses the database and schema
    information to construct a monitor request which causes the OVSDB
    node to send the controller any updates made to the OVSDB databases
    on the OVSDB node.

Passive OVSDB Node Manager Workflow
'''''''''''''''''''''''''''''''''''

A passive OVSDB node connection to OpenDaylight is made when the OVSDB
node initiates the connection to OpenDaylight. In order for this to
work, you must configure the OVSDB node to connect to the IP address and
OVSDB port on which OpenDaylight is listening. This option can be
configured on the OVSDB node using the following command:

::

    ovs-vsctl set-manager tcp:<IP address>:6640

The following diagram illustrates the sequence of events which occur
when an OVSDB node connects to OpenDaylight.

.. figure:: ./images/ovsdb-sb-passive-connection.jpg
   :alt: Passive OVSDB Manager Connection

   Passive OVSDB Manager Connection

Step 1
    The OVSDB node initiates a connection to OpenDaylight.

Step 2
    The OVSDB Southbound provider adds the OVSDB node to the OVSDB
    MD-SAL operational data store. The operational data store contains
    OVSDB node objects which represent active connections to OVSDB
    nodes.

Step 3
    The OVSDB Southbound provider requests the schema and databases
    which are supported by the OVSDB node.

Step 4
    The OVSDB Southbound provider uses the database and schema
    information to construct a monitor request which causes the OVSDB
    node to send back any updates which have been made to the OVSDB
    databases on the OVSDB node.

OVSDB Node ID in the Southbound Operational MD-SAL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When OpenDaylight initiates an active connection to an OVSDB node, it
writes an external-id to the Open\_vSwitch table on the OVSDB node. The
external-id is an OpenDaylight instance identifier which identifies the
OVSDB topology node which has just been created. Here is an example
showing the value of the *opendaylight-iid* entry in the external-ids
column of the Open\_vSwitch table where the node-id of the OVSDB node is
*ovsdb:HOST1*.

::

    $ ovs-vsctl list open_vswitch
    ...
    external_ids        : {opendaylight-iid="/network-topology:network-topology/network-topology:topology[network-topology:topology-id='ovsdb:1']/network-topology:node[network-topology:node-id='ovsdb:HOST1']"}
    ...

The *opendaylight-iid* entry in the external-ids column of the
Open\_vSwitch table causes the OVSDB node to have same node-id in the
operational MD-SAL datastore as in the configuration MD-SAL datastore.
This holds true if the OVSDB node manager settings are subsequently
changed so that a passive OVSDB manager connection is made.

If there is no *opendaylight-iid* entry in the external-ids column and a
passive OVSDB manager connection is made, then the node-id of the OVSDB
node in the operational MD-SAL datastore will be constructed using the
UUID of the Open\_vSwitch table as follows.

::

    "node-id": "ovsdb://uuid/b8dc0bfb-d22b-4938-a2e8-b0084d7bd8c1"

The *opendaylight-iid* entry can be removed from the Open\_vSwitch table
using the following command.

::

    $ sudo ovs-vsctl remove open_vswitch . external-id "opendaylight-iid"

OVSDB Changes by using OVSDB Southbound Config MD-SAL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After the connection has been made to an OVSDB node, you can make
changes to the OVSDB node by using the OVSDB Southbound Config MD-SAL.
You can make CRUD operations by using the RESTCONF interface or by a
plugin using the MD-SAL APIs. The following diagram illustrates the
high-level flow of events.

.. figure:: ./images/ovsdb-sb-config-crud.jpg
   :alt: OVSDB Changes by using the Southbound Config MD-SAL

   OVSDB Changes by using the Southbound Config MD-SAL

Step 1
    A change to the OVSDB Southbound Config MD-SAL is made. Changes
    include adding or deleting bridges and ports, or setting attributes
    of OVSDB nodes, bridges or ports.

Step 2
    The OVSDB Southbound provider receives notification of the changes
    made to the OVSDB Southbound Config MD-SAL data store.

Step 3
    As appropriate, OVSDB transactions are constructed and transmitted
    to the OVSDB node to update the OVSDB database on the OVSDB node.

Step 4
    The OVSDB node sends update messages to the OVSDB Southbound
    provider to indicate the changes made to the OVSDB nodes database.

Step 5
    The OVSDB Southbound provider maps the changes received from the
    OVSDB node into corresponding changes made to the OVSDB Southbound
    Operational MD-SAL data store.

Detecting changes in OVSDB coming from outside OpenDaylight
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Changes to the OVSDB nodes database may also occur independently of
OpenDaylight. OpenDaylight also receives notifications for these events
and updates the Southbound operational MD-SAL. The following diagram
illustrates the sequence of events.

.. figure:: ./images/ovsdb-sb-oper-crud.jpg
   :alt: OVSDB Changes made directly on the OVSDB node

   OVSDB Changes made directly on the OVSDB node

Step 1
    Changes are made to the OVSDB node outside of OpenDaylight (e.g.
    ovs-vsctl).

Step 2
    The OVSDB node constructs update messages to inform OpenDaylight of
    the changes made to its databases.

Step 3
    The OVSDB Southbound provider maps the OVSDB database changes to
    corresponding changes in the OVSDB Southbound operational MD-SAL
    data store.

OVSDB Model
^^^^^^^^^^^

The OVSDB Southbound MD-SAL operates using a YANG model which is based
on the abstract topology node model found in the `network topology
model <https://github.com/opendaylight/yangtools/blob/stable/boron/model/ietf/ietf-topology/src/main/yang/network-topology%402013-10-21.yang>`__.

The augmentations for the OVSDB Southbound MD-SAL are defined in the
`ovsdb.yang <https://github.com/opendaylight/ovsdb/blob/stable/boron/southbound/southbound-api/src/main/yang/ovsdb.yang>`__
file.

There are three augmentations:

**ovsdb-node-augmentation**
    This augments the topology node and maps primarily to the
    Open\_vSwitch table of the OVSDB schema. It contains the following
    attributes.

    -  **connection-info** - holds the local and remote IP address and
       TCP port numbers for the OpenDaylight to OVSDB node connections

    -  **db-version** - version of the OVSDB database

    -  **ovs-version** - version of OVS

    -  **list managed-node-entry** - a list of references to
       ovsdb-bridge-augmentation nodes, which are the OVS bridges
       managed by this OVSDB node

    -  **list datapath-type-entry** - a list of the datapath types
       supported by the OVSDB node (e.g. *system*, *netdev*) - depends
       on newer OVS versions

    -  **list interface-type-entry** - a list of the interface types
       supported by the OVSDB node (e.g. *internal*, *vxlan*, *gre*,
       *dpdk*, etc.) - depends on newer OVS verions

    -  **list openvswitch-external-ids** - a list of the key/value pairs
       in the Open\_vSwitch table external\_ids column

    -  **list openvswitch-other-config** - a list of the key/value pairs
       in the Open\_vSwitch table other\_config column

**ovsdb-bridge-augmentation**
    This augments the topology node and maps to an specific bridge in
    the OVSDB bridge table of the associated OVSDB node. It contains the
    following attributes.

    -  **bridge-uuid** - UUID of the OVSDB bridge

    -  **bridge-name** - name of the OVSDB bridge

    -  **bridge-openflow-node-ref** - a reference (instance-identifier)
       of the OpenFlow node associated with this bridge

    -  **list protocol-entry** - the version of OpenFlow protocol to use
       with the OpenFlow controller

    -  **list controller-entry** - a list of controller-uuid and
       is-connected status of the OpenFlow controllers associated with
       this bridge

    -  **datapath-id** - the datapath ID associated with this bridge on
       the OVSDB node

    -  **datapath-type** - the datapath type of this bridge

    -  **fail-mode** - the OVSDB fail mode setting of this bridge

    -  **flow-node** - a reference to the flow node corresponding to
       this bridge

    -  **managed-by** - a reference to the ovsdb-node-augmentation
       (OVSDB node) that is managing this bridge

    -  **list bridge-external-ids** - a list of the key/value pairs in
       the bridge table external\_ids column for this bridge

    -  **list bridge-other-configs** - a list of the key/value pairs in
       the bridge table other\_config column for this bridge

**ovsdb-termination-point-augmentation**
    This augments the topology termination point model. The OVSDB
    Southbound MD-SAL uses this model to represent both the OVSDB port
    and OVSDB interface for a given port/interface in the OVSDB schema.
    It contains the following attributes.

    -  **port-uuid** - UUID of an OVSDB port row

    -  **interface-uuid** - UUID of an OVSDB interface row

    -  **name** - name of the port

    -  **interface-type** - the interface type

    -  **list options** - a list of port options

    -  **ofport** - the OpenFlow port number of the interface

    -  **ofport\_request** - the requested OpenFlow port number for the
       interface

    -  **vlan-tag** - the VLAN tag value

    -  **list trunks** - list of VLAN tag values for trunk mode

    -  **vlan-mode** - the VLAN mode (e.g. access, native-tagged,
       native-untagged, trunk)

    -  **list port-external-ids** - a list of the key/value pairs in the
       port table external\_ids column for this port

    -  **list interface-external-ids** - a list of the key/value pairs
       in the interface table external\_ids interface for this interface

    -  **list port-other-configs** - a list of the key/value pairs in
       the port table other\_config column for this port

    -  **list interface-other-configs** - a list of the key/value pairs
       in the interface table other\_config column for this interface

Examples of OVSDB Southbound MD-SAL API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Connect to an OVSDB Node
^^^^^^^^^^^^^^^^^^^^^^^^

This example RESTCONF command adds an OVSDB node object to the OVSDB
Southbound configuration data store and attempts to connect to the OVSDB
host located at the IP address 10.11.12.1 on TCP port 6640.

::

    POST http://<host>:8181/restconf/config/network-topology:network-topology/topology/ovsdb:1/
    Content-Type: application/json
    {
      "node": [
         {
           "node-id": "ovsdb:HOST1",
           "connection-info": {
             "ovsdb:remote-ip": "10.11.12.1",
             "ovsdb:remote-port": 6640
           }
         }
      ]
    }

Query the OVSDB Southbound Configuration MD-SAL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Following on from the previous example, if the OVSDB Southbound
configuration MD-SAL is queried, the RESTCONF command and the resulting
reply is similar to the following example.

::

    GET http://<host>:8080/restconf/config/network-topology:network-topology/topology/ovsdb:1/
    Application/json data in the reply
    {
      "topology": [
        {
          "topology-id": "ovsdb:1",
          "node": [
            {
              "node-id": "ovsdb:HOST1",
              "ovsdb:connection-info": {
                "remote-port": 6640,
                "remote-ip": "10.11.12.1"
              }
            }
          ]
        }
      ]
    }

Reference Documentation
~~~~~~~~~~~~~~~~~~~~~~~

`Openvswitch
schema <http://openvswitch.org/ovs-vswitchd.conf.db.5.pdf>`__

OVSDB Hardware VTEP Developer Guide
-----------------------------------

Overview
~~~~~~~~

TBD

OVSDB Hardware VTEP Architecture
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TBD
