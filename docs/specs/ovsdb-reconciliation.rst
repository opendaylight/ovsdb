
.. contents:: Table of Contents
      :depth: 3

=====================
OVSDB Reconciliation
=====================

https://git.opendaylight.org/gerrit/#/q/topic:ovsdb-reconciliation

Add support for reconciliation in ovsdb for tunnel interfaces and bridges.


Problem description
===================

OVSDB termination point create reconciliation is supported now.
But OVSDB termination point delete reconciliation is not supported,
because of that there is possibility of stale teps and associated
problems, during ovs disconnects and controller reboot.

Use Cases
---------

* UC 1: Reconciliation after controller reboot
* UC 2: Reconciliation after switch re-connection

Proposed change
===============

Controller will stamp the resources created by controller, i.e resources
created by controller will be programmed with external id probably looking
like "openvswitch:external_ids:resource_owner:ODL", when switch
connects back we will compare what is reported by ovsdb and what is
there in config db for resources stamped owner as controller, and we do
reconciliation based on this.

Pipeline changes
----------------
None.

Yang changes
------------
None.

Configuration impact
---------------------
None

Clustering considerations
-------------------------
None

Other Infra considerations
--------------------------
None

Security considerations
-----------------------
None

Scale and Performance Impact
----------------------------
None

Targeted Release
-----------------
Fluorine

Alternatives
------------
N.A.

Usage
=====

Features to Install
-------------------
This feature doesn't add any new karaf feature.

REST API
--------
None.

CLI
---
None


Implementation
==============

Assignee(s)
-----------

Primary assignee:
  Nobin Mathew


Work Items
----------
#. Stamp resource created by controller with external_ids key "opendaylight-iid" and value "TBD"
#. Add TEP delete reconciliation in OVSDB plugin

Dependencies
============
None

Testing
=======

Unit Tests
----------
Appropriate UTs will be added for the new code coming in once framework is in place


Integration Tests
-----------------
There won't be any Integration tests provided for this feature.


CSIT
----
None

Documentation Impact
====================
None.

References
==========
[1] Fluorine DDF slides https://docs.google.com/presentation/d/1qLHdw3Hj5piv5eyQetzna-Gk-GndUa5C1pC-jIrnkPs/edit#slide=id.g35ab225711_0_69

[2] OVSDB SB Reconciliation https://wiki.opendaylight.org/view/OVSDB_Integration:OVSDB_SB_Reconciliation

[3] Bug 5951: Termination point config reconciliation https://trello.com/c/ISZ4MTNs/74-bug-5951-termination-point-config-reconciliation

[4] Gerrit: Termination point config reconciliation https://git.opendaylight.org/gerrit/#/c/40506/