package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;

public interface ISfcClassifierService {
    void programIngressClassifier(long dataPathId, String ruleName, Matches matches,
                                  NshUtils nshHeader, long vxGpeOfPort, boolean write);
}
