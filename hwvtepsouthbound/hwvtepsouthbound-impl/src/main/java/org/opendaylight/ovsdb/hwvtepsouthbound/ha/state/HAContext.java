package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;


public class HAContext {

    static final Logger LOG = LoggerFactory.getLogger(HAContext.class);
    Node node;
    Node psNode;
    String switchName = "";
    String haId = "";
    Optional<NodeId> nodeIdOptional = Optional.absent();
    boolean nodeAdded = false;
    boolean isHaEnabled = false;
    InstanceIdentifier<Node> nodePath;
    InstanceIdentifier<Node> nodePsPath;
    InstanceIdentifier<Node> haNodePath;
    InstanceIdentifier<Node> haPsPath;

    Optional<Node> haGlobalOperNodeOptional = Optional.absent();
    Optional<Node> haSwitchOperNodeOptional = Optional.absent();

    Optional<Node> haGlobalConfigNodeOptional = Optional.absent();
    Optional<Node> haSwitchConfigNodeOptional = Optional.absent();

    Optional<Node> globalConfigNodeOptional = Optional.absent();
    Optional<Node> switchConfigNodeOptional = Optional.absent();
    ReadWriteTransaction tx ;

    public InstanceIdentifier<Node> getHaNodePath() {
        return haNodePath;
    }

    public HAContext(Node node, Node psNode, boolean nodeAdded, ReadWriteTransaction tx) throws Exception {
        this.node   = node;
        this.psNode = psNode;
        this.tx = tx;
        this.nodeAdded = nodeAdded;
        loadContext(tx);
    }

    public HAState getHaState() throws Exception {
        if (!isHaEnabled) {
            return HAState.NonHA;
        }
        if (nodeAdded) {
            haGlobalConfigNodeOptional = tx.read(CONFIGURATION, haNodePath).checkedGet();
            if (!haGlobalConfigNodeOptional.isPresent()) {
                return HAState.D1Connected;
            }

            haGlobalOperNodeOptional = tx.read(OPERATIONAL, haNodePath).checkedGet();
            if (!haGlobalOperNodeOptional.isPresent()) {
                return HAState.D1ReConnected;
            }

            globalConfigNodeOptional = tx.read(CONFIGURATION, nodePath).checkedGet();
            if (!globalConfigNodeOptional.isPresent()) {
                return HAState.D2Connected;
            } else {
                return HAState.D2Reconnected;
            }
        }
        haGlobalConfigNodeOptional = tx.read(CONFIGURATION, haNodePath).checkedGet();
        haGlobalOperNodeOptional = tx.read(OPERATIONAL, haNodePath).checkedGet();
        return HAState.D1Disconnected;
    }

    Node getPsNode(ReadWriteTransaction tx) throws Exception {
        if (psNode != null)
            return psNode;
        InstanceIdentifier<Topology> topoPath = nodePath.firstIdentifierOf(Topology.class);
        Optional<Topology> topologyOptional = tx.read(OPERATIONAL, topoPath).checkedGet();
        if (!topologyOptional.isPresent()) {
            LOG.error("failed to find topolog op");
            return psNode;
        }
        for (Node topoNode : topologyOptional.get().getNode()) {
            PhysicalSwitchAugmentation augmentation = topoNode.getAugmentation(PhysicalSwitchAugmentation.class);
            if (augmentation != null && augmentation.getManagedBy() != null ) {
                if (nodePath.equals(augmentation.getManagedBy().getValue())) {
                    psNode = topoNode;
                }
            }
        }
        return psNode;
    }

    public String getHaId() {
        HwvtepGlobalAugmentation globalAugmentation =
                node.getAugmentation(HwvtepGlobalAugmentation.class);
        if (globalAugmentation != null) {
            haId = HAUtil.getHAId(null/*db*/, globalAugmentation);
        }
        return haId;
    }

    public void loadContext(ReadWriteTransaction tx) throws Exception {
        if (node == null) {
            if (psNode != null) {
                PhysicalSwitchAugmentation physicalSwitchAugmentation =
                        psNode.getAugmentation(PhysicalSwitchAugmentation.class);
                nodePath = (InstanceIdentifier<Node>) physicalSwitchAugmentation.getManagedBy().getValue();
                Optional<Node> nodeOptional = tx.read(OPERATIONAL, nodePath).checkedGet();
                if (!nodeOptional.isPresent()) {
                    LOG.error("node not yet present in operational ds skipping this psNode node");
                    return;
                }
                node = nodeOptional.get();
            } else {
                return;
            }
        }

        nodePath    = HAUtil.createInstanceIdentifier(node.getNodeId().getValue());
        nodeIdOptional = Optional.of(node.getNodeId());
        if (nodeAdded && getPsNode(tx) == null) {
            LOG.error("PS node not yet present in operational ds skipping this node");
            return;
        }

        haId = getHaId();
        if (Strings.isNullOrEmpty(haId)) {
            return;
        }

        isHaEnabled = true;

        haNodePath  = HAUtil.getInstanceIdentifier(haId);
        NodeId haNodeId = haNodePath.firstKeyOf(Node.class).getNodeId();
        if (nodeAdded && psNode != null) {
            String psIdVal = psNode.getNodeId().getValue();
            switchName = psIdVal.substring(psIdVal.indexOf("physicalswitch") + "physicalswitch".length() + 1);

            nodePsPath = HAUtil.createInstanceIdentifier(psNode.getNodeId().getValue());

            String haPsNodeIdVal = haNodeId.getValue() + "/physicalswitch/" + switchName;

            haPsPath = HAUtil.createInstanceIdentifier(haPsNodeIdVal);
        }

    }

}

