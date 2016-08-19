package org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface MergerInterface {

    public void mergeChildSwitchOperationalToHA(
            Node psNode,
            InstanceIdentifier<Node> haNodePath,
            InstanceIdentifier<Node> haPsPath,
            ReadWriteTransaction tx) throws Exception;

    void mergeChildGlobalOperationalToHA(Node srcNode,
                                         InstanceIdentifier<Node> dstNodePath,
                                         ReadWriteTransaction tx)  throws Exception;


    void mergeHASwitchConfigToChild(Optional<Node> haPsConfigNodeOptional,
                                    InstanceIdentifier<Node> psNodPath,
                                    InstanceIdentifier<Node> nodePath,
                                    ReadWriteTransaction tx) throws Exception;

    void mergeHAGlobalConfigToChild(Node srcNode,
                                    InstanceIdentifier<Node> dstNodePath,
                                    ReadWriteTransaction tx) throws Exception;

}
