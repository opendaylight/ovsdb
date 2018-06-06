
.. contents:: Table of Contents
      :depth: 3

=====================
OVSDB Reconciliation
=====================

https://git.opendaylight.org/gerrit/#/q/topic:ovsdb-reconciliation

Add support for reconciliation in ovsdb for tunnel interfaces and bridges.


Problem description
===================

We don't want anybody to tinker with switch configuration for entities
owned by the controller, switch configuration should be identical
to what is there in the controller config.

Use Cases
---------
The following usecases are supported

* Use case 1: Reconciliation after controller reboot
* Use case 2: Reconciliation after switch re-connection
* Use case 3: Reconciliation after controller upgrade
* Use case 4: Reconciliation after switch connects back to
  different controller in a cluster of controllers
* Use case 5: Reconciliation should happen in both controller
  initiated and switch initiated connections


Proposed change
===============

Controller should do reconciliation only on entities owned by the controller.
For this we need to identify entities created by controller. Currently we use
the key-value pair "opendaylight-iid:"operational datastore node Id" to stamp
the entities created by the controller, but for a future proof solution this
feature will add another key-value pair "created_by:odl" to stamp the entities
created by the controller. This key-value pair will go in the external_ids
column of ovsdb tables in switch.

This feature will support only create and delete reconciliation, create
reconciliation is already implemented, see references 3 & 4. Update reconciliation
is not supported in this feature. Bridge and tunnel interface delete reconciliations
are handled in this feature. This feature should handle both switch initiated and
controller initiated connections.

In order to handle reconciliation in a generic way, we will first reconcile,
then publish bridge to network topology Operational DS, current implementation does this
in parallel. This can be a seperate reconciliation path where we compare config
and oper DS, or we can do it in transact commands.

**Handling various scenarios**

* Non upgrade scenario
   - New switch connects: There is no configuration in the controller and switch,
     first reconciliation kicks in, nothing to reconcile, then publishes switch
     to oper DS.

   - Existing switch flaps: There is same configuration in both controller and switch,
     reconciliation code retains the same in switch, then switch gets published to OperDS

   - Existing switch flaps with some change in configuration: Here create reconciliation
     should push the new configuration to switch, and delete reconciliation should delete
     the old ones, and push the switch to Oper DS.

* Auto-Tunnel scenario (non upgrade)
   - New switch connects: same as above

   - Existing switch flaps: same as above

   - Existing switch flaps with change in tep/tz configuration: delete reconciliation should
     delete the old tunnels and publish to Oper DS. Or should we just allow auto-tunnel module
     to take care of this?

* Upgrade scenario (replay based upgrade)
   - New switch connects: nothing to reconcile, switch is published to Oper DS

   - existing switch connects back after controller upgrade: we will not do reconciliation
     till the upgrade complete flag is set, once this flag is set, reconciliation kicks in
     and switch is published to OperDS

Pipeline changes
----------------
None. This is OVSDB plugin

Yang changes
------------
None.

Configuration impact
---------------------
None

Clustering considerations
-------------------------
Reconciliation should work in a clustered environment.

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
Installing OVSDB, genius or netvirt will install this feature.

REST API
--------
No new REST APIs are added.

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
#. Add code to stamp entities created by controller by key-value pair
#. Add reconciliation
#. Add unit test cases for new reconciliation code
#. Scale test and measure the performance
#. Add CSIT for reconciliation

Dependencies
============
None

Testing
=======

Unit Tests
----------
Appropriate UTs will be added for the new code coming in


Integration Tests
-----------------
None


CSIT
----
Necessary CSIT tests will be added

We need to perform these tests for both single node and cluster cases


Documentation Impact
====================
None.

References
==========
[1] Fluorine DDF slides https://docs.google.com/presentation/d/1qLHdw3Hj5piv5eyQetzna-Gk-GndUa5C1pC-jIrnkPs/edit#slide=id.g35ab225711_0_69

[2] OVSDB SB Reconciliation https://wiki.opendaylight.org/view/OVSDB_Integration:OVSDB_SB_Reconciliation

[3] Bug 5951: Termination point config reconciliation https://trello.com/c/ISZ4MTNs/74-bug-5951-termination-point-config-reconciliation

[4] Gerrit: Termination point config reconciliation https://git.opendaylight.org/gerrit/#/c/40506/