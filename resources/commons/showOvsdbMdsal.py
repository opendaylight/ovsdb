#!/usr/bin/env python

import urllib2, base64, json, sys, optparse

# globals
CONST_DEFAULT_DEBUG=0
options = None
state = None
jsonTopologyNodes = []
jsonInventoryNodes = []
flowInfoNodes = {}
nodeIdToDpidCache = {}

CONST_OPERATIONAL = 'operational'
CONST_CONFIG = 'config'
CONST_NET_TOPOLOGY = 'network-topology'
CONST_TOPOLOGY = 'topology'
CONST_TP_OF_INTERNAL = 65534
CONST_ALIASES = ['alpha', 'bravo', 'charlie', 'delta', 'echo', 'foxtrot', 'golf', 'hotel', 'india', 'juliet',
                 'kilo', 'lima', 'mike', 'november', 'oscar', 'papa', 'quebec', 'romeo', 'sierra', 'tango',
                 'uniform', 'victor', 'whiskey', 'xray', 'yankee', 'zulu']


class State:
    def __init__(self):
        self.nextAliasIndex = 0
        self.nextAliasWrap = 0
        self.nodeIdToAlias = {}

        self.bridgeNodes = {}
        self.ovsdbNodes = {}
        self.ofLinks = {}

    def __repr__(self):
        return 'State {}:{} {}:{} {}:{} {}:{} {}:{}'.format(
            'nextAliasIndex', self.nextAliasIndex,
            'nextAliasWrap', self.nextAliasWrap,
            'bridgeNodes_ids', self.bridgeNodes.keys(),
            'ovsdbNodes_ids', self.ovsdbNodes.keys(),
            'nodeIdToAlias', self.nodeIdToAlias)

    def registerBridgeNode(self, bridgeNode):
        self.bridgeNodes[bridgeNode.nodeId] = bridgeNode

    def registerOvsdbNode(self, ovsdbNode):
        self.ovsdbNodes[ovsdbNode.nodeId] = ovsdbNode

    def getNextAlias(self, nodeId):
        result = CONST_ALIASES[ self.nextAliasIndex ]
        if self.nextAliasWrap > 0:
            result += '_' + str(self.nextAliasWrap)

        if CONST_ALIASES[ self.nextAliasIndex ] == CONST_ALIASES[-1]:
            self.nextAliasIndex = 0
            self.nextAliasWrap += 1
        else:
            self.nextAliasIndex += 1

        self.nodeIdToAlias[ nodeId ] = result
        return result

# --

class TerminationPoint:
    def __init__(self, name, ofPort, tpType, mac='', ifaceId=''):
        self.name = name
        self.ofPort = ofPort
        self.tpType = tpType
        self.mac = mac
        self.ifaceId = ifaceId

    def __repr__(self):
        result = '{} {}:{}'.format(self.name, 'of', self.ofPort)

        if self.tpType != '':
            result += ' {}:{}'.format('type', self.tpType)
        if self.mac != '':
            result += ' {}:{}'.format('mac', self.mac)
        if self.ifaceId != '':
            result += ' {}:{}'.format('ifaceId', self.ifaceId)

        return '{' + result + '}'

# --

class BridgeNode:
    def __init__(self, nodeId, dpId, name, controllerTarget, controllerConnected):
        global state
        self.alias = state.getNextAlias(nodeId)
        self.nodeId = nodeId
        self.dpId = dpId
        self.name = name
        self.controllerTarget = controllerTarget
        self.controllerConnected = controllerConnected
        self.tps = []

    def getOpenflowName(self):
        if self.dpId is None:
            return self.nodeId
        return dataPathIdToOfFormat(self.dpId)

    def addTerminationPoint(self, terminationPoint):
        self.tps.append(terminationPoint)

    def __repr__(self):
        return 'BridgeNode {}:{} {}:{} {}:{} {}:{} {}:{} {}:{} {}:{} {}:{}'.format(
            'alias', self.alias,
            'nodeId', self.nodeId,
            'dpId', self.dpId,
            'openflowName', self.getOpenflowName(),
            'name', self.name,
            'controllerTarget', self.controllerTarget,
            'controllerConnected', self.controllerConnected,
            'tps', self.tps)

# --

class OvsdbNode:
    def __init__(self, nodeId, inetMgr, inetNode, otherLocalIp, ovsVersion):
        global state
        if inetNode != '':
            self.alias = inetNode
        else:
            self.alias = nodeId
        self.nodeId = nodeId
        self.inetMgr = inetMgr
        self.inetNode = inetNode
        self.otherLocalIp = otherLocalIp
        self.ovsVersion = ovsVersion

    def __repr__(self):
        return 'OvsdbNode {}:{} {}:{} {}:{} {}:{} {}:{} {}:{}'.format(
            'alias', self.alias,
            'nodeId', self.nodeId,
            'inetMgr', self.inetMgr,
            'inetNode', self.inetNode,
            'otherLocalIp', self.otherLocalIp,
            'ovsVersion', self.ovsVersion)

# ======================================================================

def printError(msg):
    sys.stderr.write(msg)

# ======================================================================

def prt(msg, logLevel=0):
    prtCommon(msg, logLevel)
def prtLn(msg, logLevel=0):
    prtCommon('{}\n'.format(msg), logLevel)
def prtCommon(msg, logLevel):
    if options.debug >= logLevel:
        sys.stdout.write(msg)

# ======================================================================

def getMdsalTreeType():
    if options.useConfigTree:
        return CONST_CONFIG
    return CONST_OPERATIONAL

# --

def grabJson(url):

    try:
        request = urllib2.Request(url)
        # You need the replace to handle encodestring adding a trailing newline
        # (https://docs.python.org/2/library/base64.html#base64.encodestring)
        base64string = base64.encodestring('{}:{}'.format(options.odlUsername, options.odlPassword)).replace('\n', '')
        request.add_header('Authorization', 'Basic {}'.format(base64string))
        result = urllib2.urlopen(request)
    except urllib2.URLError, e:
        printError('Unable to send request: {}\n'.format(e))
        sys.exit(1)

    if (result.code != 200):
        printError( '{}\n{}\n\nError: unexpected code: {}\n'.format(result.info(), result.read(), result.code) )
        sys.exit(1)

    data = json.load(result)
    prtLn(data, 4)
    return data

# --

def grabInventoryJson(mdsalTreeType):
    global jsonInventoryNodes

    url = 'http://{}:{}/restconf/{}/opendaylight-inventory:nodes/'.format(options.odlIp, options.odlPort, mdsalTreeType)
    data = grabJson(url)

    if not 'nodes' in data:
        printError( '{}\n\nError: did not find nodes in {}'.format(data, url) )
        sys.exit(1)

    data2 = data['nodes']
    if not 'node' in data2:
        printError( '{}\n\nError: did not find node in {}'.format(data2, url) )
        sys.exit(1)

    jsonInventoryNodes = data2['node']

# --

def parseInventoryJson(mdsalTreeType):
    global jsonInventoryNodes
    global flowInfoNodes

    for nodeDict in jsonInventoryNodes:
        if not 'id' in nodeDict:
            continue

        bridgeOfId = nodeDict.get('id')
        prtLn('inventory node {} has keys {}'.format(bridgeOfId, nodeDict.keys()), 3)

        # locate bridge Node
        bridgeNodeId = None
        bridgeNode = None
        for currNodeId in state.bridgeNodes.keys():
            if state.bridgeNodes[currNodeId].getOpenflowName() == bridgeOfId:
                bridgeNodeId = currNodeId
                bridgeNode = state.bridgeNodes[currNodeId]
                break

        if bridgeNodeId is None:
            prtLn('inventory node {}'.format(bridgeOfId), 1)
        else:
            prtLn('inventory node {}, aka {}, aka {}'.format(bridgeOfId, bridgeNodeId, showPrettyName(bridgeNodeId)), 1)

        flowInfoNode = {}

        indent = ' ' * 2
        prtLn('{}features: {}'.format(indent, nodeDict.get('flow-node-inventory:switch-features', {})), 2)
        prt('{}sw: {}'.format(indent, nodeDict.get('flow-node-inventory:software')), 2)
        prt('{}hw: {}'.format(indent, nodeDict.get('flow-node-inventory:hardware')), 2)
        prt('{}manuf: {}'.format(indent, nodeDict.get('flow-node-inventory:manufacturer')), 2)
        prtLn('{}ip: {}'.format(indent, nodeDict.get('flow-node-inventory:ip-address')), 2)


        for inventoryEntry in nodeDict.get('flow-node-inventory:table', []):
            if 'id' in inventoryEntry:
                currTableId = inventoryEntry.get('id')
                for currFlow in inventoryEntry.get('flow', []):
                    prtLn('{}table {}: {}'.format(indent, currTableId, currFlow.get('id')), 1)
                    prtLn('{}{}'.format(indent * 2, currFlow), 2)

                    if currTableId in flowInfoNode:
                        flowInfoNode[ currTableId ].append( currFlow.get('id') )
                    else:
                        flowInfoNode[ currTableId ] = [ currFlow.get('id') ]

        prtLn('', 1)

        for currTableId in flowInfoNode.keys():
            flowInfoNode[currTableId].sort()

        # store info collected in flowInfoNodes
        flowInfoNodes[bridgeOfId] = flowInfoNode

# --

def grabTopologyJson(mdsalTreeType):
    global jsonTopologyNodes

    url = 'http://{}:{}/restconf/{}/network-topology:network-topology/'.format(options.odlIp, options.odlPort, mdsalTreeType)
    data = grabJson(url)

    if not CONST_NET_TOPOLOGY in data:
        printError( '{}\n\nError: did not find {} in data'.format(data, CONST_NET_TOPOLOGY) )
        sys.exit(1)

    data2 = data[CONST_NET_TOPOLOGY]
    if not CONST_TOPOLOGY in data2:
        printError( '{}\n\nError: did not find {} in data2'.format(data2, CONST_TOPOLOGY) )
        sys.exit(1)

    jsonTopologyNodes = data2[CONST_TOPOLOGY]

# --

def buildDpidCache():
    global jsonTopologyNodes
    global nodeIdToDpidCache

    # only needed if not parsing operational tree
    if getMdsalTreeType() == CONST_OPERATIONAL:
        return

    jsonTopologyNodesSave = jsonTopologyNodes
    grabTopologyJson(CONST_OPERATIONAL)
    jsonTopologyNodesLocal = jsonTopologyNodes
    jsonTopologyNodes = jsonTopologyNodesSave

    for nodeDict in jsonTopologyNodesLocal:
        if nodeDict.get('topology-id') != 'ovsdb:1':
            continue
        for node in nodeDict.get('node', []):
            if node.get('node-id') is None or node.get('ovsdb:datapath-id') is None:
                continue
            nodeIdToDpidCache[ node.get('node-id') ] = node.get('ovsdb:datapath-id')

# --

def parseTopologyJson(mdsalTreeType):
    for nodeDict in jsonTopologyNodes:
        if not 'topology-id' in nodeDict:
            continue
        prtLn('{} {} keys are: {}'.format(mdsalTreeType, nodeDict['topology-id'], nodeDict.keys()), 3)
        if 'node' in nodeDict:
            nodeIndex = 0
            for currNode in nodeDict['node']:
                parseTopologyJsonNode('', mdsalTreeType, nodeDict['topology-id'], nodeIndex, currNode)
                nodeIndex += 1
            prtLn('', 2)
        if (mdsalTreeType == CONST_OPERATIONAL) and (nodeDict['topology-id'] == 'flow:1') and ('link' in nodeDict):
            parseTopologyJsonFlowLink(nodeDict['link'])

    prtLn('', 1)

# --

def parseTopologyJsonNode(indent, mdsalTreeType, topologyId, nodeIndex, node):
    if node.get('node-id') is None:
        printError( 'Warning: unexpected node: {}\n'.format(node) )
        return
    prt('{} {} node[{}] {} '.format(indent + mdsalTreeType, topologyId, nodeIndex, node.get('node-id')), 2)
    if 'ovsdb:bridge-name' in node:
        prtLn('', 2)
        parseTopologyJsonNodeBridge(indent + '  ', mdsalTreeType, topologyId, nodeIndex, node)
    elif 'ovsdb:connection-info' in node:
        prtLn('', 2)
        parseTopologyJsonNodeOvsdb(indent + '  ', mdsalTreeType, topologyId, nodeIndex, node)
    else:
        prtLn('keys: {}'.format(node.keys()), 2)

# --

def parseTopologyJsonNodeOvsdb(indent, mdsalTreeType, topologyId, nodeIndex, node):
    keys = node.keys()
    keys.sort()
    for k in keys:
        prtLn('{}{} : {}'.format(indent, k, node[k]), 2)

    connectionInfoRaw = node.get('ovsdb:connection-info')
    connectionInfo = {}
    if type(connectionInfoRaw) is dict:
        connectionInfo['inetMgr'] = connectionInfoRaw.get('local-ip') + ':' + str( connectionInfoRaw.get('local-port') )
        connectionInfo['inetNode'] = connectionInfoRaw.get('remote-ip') + ':' + str( connectionInfoRaw.get('remote-port') )
    otherConfigsRaw = node.get('ovsdb:openvswitch-other-configs')
    otherLocalIp = ''
    if type(otherConfigsRaw) is list:
        for currOtherConfig in otherConfigsRaw:
            if type(currOtherConfig) is dict and \
                    currOtherConfig.get('other-config-key') == 'local_ip':
                otherLocalIp = currOtherConfig.get('other-config-value')
                break

    ovsdbNode = OvsdbNode(node.get('node-id'), connectionInfo.get('inetMgr'), connectionInfo.get('inetNode'), otherLocalIp, node.get('ovsdb:ovs-version'))
    state.registerOvsdbNode(ovsdbNode)
    prtLn('Added {}'.format(ovsdbNode), 1)

# --

def parseTopologyJsonNodeBridge(indent, mdsalTreeType, topologyId, nodeIndex, node):

    controllerTarget = None
    controllerConnected = None
    controllerEntries = node.get('ovsdb:controller-entry')
    if type(controllerEntries) is list:
        for currControllerEntry in controllerEntries:
            if type(currControllerEntry) is dict:
                controllerTarget = currControllerEntry.get('target')
                controllerConnected = currControllerEntry.get('is-connected')
                break

    nodeId = node.get('node-id')
    dpId = node.get('ovsdb:datapath-id', nodeIdToDpidCache.get(nodeId))
    bridgeNode = BridgeNode(nodeId, dpId, node.get('ovsdb:bridge-name'), controllerTarget, controllerConnected)

    keys = node.keys()
    keys.sort()
    for k in keys:
        if k == 'termination-point' and len(node[k]) > 0:
            tpIndex = 0
            for tp in node[k]:
                terminationPoint = parseTopologyJsonNodeBridgeTerminationPoint('%stermination-point[%d] :' % (indent, tpIndex), mdsalTreeType, topologyId, nodeIndex, node, tp)

                # skip boring tps
                if terminationPoint.ofPort == CONST_TP_OF_INTERNAL and \
                        (terminationPoint.name == 'br-ex' or terminationPoint.name == 'br-int'):
                    pass
                else:
                    bridgeNode.addTerminationPoint(terminationPoint)

                tpIndex += 1
        else:
            prtLn('{}{} : {}'.format(indent, k, node[k]), 2)

    state.registerBridgeNode(bridgeNode)
    prtLn('Added {}'.format(bridgeNode), 1)


# --

def parseTopologyJsonNodeBridgeTerminationPoint(indent, mdsalTreeType, topologyId, nodeIndex, node, tp):
    attachedMac = ''
    ifaceId = ''

    keys = tp.keys()
    keys.sort()
    for k in keys:
        if (k == 'ovsdb:port-external-ids' or k == 'ovsdb:interface-external-ids') and len(tp[k]) > 0:
            extIdIndex = 0
            for extId in tp[k]:
                prtLn('{} {}[{}] {} : {}'.format(indent, k, extIdIndex, extId.get('external-id-key'), extId.get('external-id-value')), 2)
                extIdIndex += 1

                if extId.get('external-id-key') == 'attached-mac':
                    attachedMac = extId.get('external-id-value')
                if extId.get('external-id-key') == 'iface-id':
                    ifaceId = extId.get('external-id-value')
        else:
            prtLn('{} {} : {}'.format(indent, k, tp[k]), 2)

    return TerminationPoint(tp.get('ovsdb:name'),
                            tp.get('ovsdb:ofport'),
                            tp.get('ovsdb:interface-type', '').split('-')[-1],
                            attachedMac, ifaceId)

# --

def parseTopologyJsonFlowLink(link):
    linkCount = 0
    spc = ' ' * 2
    for currLinkDict in link:
        linkCount += 1
        linkId = currLinkDict.get('link-id')
        linkDest = currLinkDict.get('destination', {}).get('dest-tp')
        linkSrc = currLinkDict.get('source', {}).get('source-tp')

        linkDestNode = currLinkDict.get('destination', {}).get('dest-node')
        linkSrcNode = currLinkDict.get('source', {}).get('source-node')
        prtLn('{} {} {} => {}:{} -> {}:{}'.format(spc, linkCount, linkId, linkSrcNode, linkSrc.split(':')[-1], linkDestNode, linkDest.split(':')[-1]), 3)

        if linkId != linkSrc:
            printError('Warning: ignoring link with unexpected id: %s != %s\n' % (linkId, linkSrc))
            continue
        else:
            state.ofLinks[linkSrc] = linkDest

# --

def showPrettyNamesMap():
    spc = ' ' * 2
    if not options.useAlias or len(state.bridgeNodes) == 0:
        return

    prtLn('aliasMap:', 0)
    resultMap = {}
    for bridge in state.bridgeNodes.values():
        resultMap[ bridge.alias ] = '{0: <25} {1: <7} {2}'.format(bridge.getOpenflowName(), bridge.name, bridge.dpId)

    for resultMapKey in sorted(resultMap):
        prtLn('{0}{1: <10} ->  {2}'.format(spc, resultMapKey, resultMap[resultMapKey]), 0)
    prtLn('', 0)

# --

def showNodesPretty():
    if len(state.ovsdbNodes) == 0:
        showBridgeOnlyNodes()
        return

    aliasDict = { state.ovsdbNodes[nodeId].alias : nodeId for nodeId in state.ovsdbNodes.keys() }
    aliasDictKeys = aliasDict.keys()
    aliasDictKeys.sort()
    for ovsAlias in aliasDictKeys:
        ovsdbNode = state.ovsdbNodes[ aliasDict[ovsAlias] ]

        prt('ovsdbNode:{} mgr:{} version:{}'.format(ovsAlias, ovsdbNode.inetMgr, ovsdbNode.ovsVersion), 0)
        if ovsdbNode.inetNode.split(':')[0] != ovsdbNode.otherLocalIp:
            prt(' **localIp:{}'.format(ovsdbNode.otherLocalIp), 0)
        prtLn('', 0)
        showPrettyBridgeNodes('  ', getNodeBridgeIds(ovsdbNode.nodeId), ovsdbNode)
    showBridgeOnlyNodes(True)
    prtLn('', 0)

# --

def showFlowInfoPretty():
    global flowInfoNodes
    spc = ' ' * 2

    if not options.showFlows:
        return

    if len(flowInfoNodes) == 0:
        prtLn('no flowInfo found\n', 0)
        return

    # translate flowKeys (openflow:123124) into their alias format
    # then sort it and translate back, so we list them in the order
    flowInfoNodeKeysDict = {}
    for flowInfoNodeKey in flowInfoNodes.keys():
        flowInfoNodeKeysDict[ showPrettyName(flowInfoNodeKey) ] = flowInfoNodeKey
    flowInfoNodeKeysKeys = flowInfoNodeKeysDict.keys()
    flowInfoNodeKeysKeys.sort()

    flowInfoNodesKeys = [ flowInfoNodeKeysDict[ x ] for x in flowInfoNodeKeysKeys ]

    nodeIdToDpidCacheReverse = {dataPathIdToOfFormat(v): k for k, v in nodeIdToDpidCache.items()}
    nodesVisited = 0
    for flowInfoNodeKey in flowInfoNodesKeys:
        if nodesVisited > 0: prtLn('', 0)

        nodeName = showPrettyName(flowInfoNodeKey)
        if nodeName == flowInfoNodeKey:
            nodeName += '  ( {} )'.format( nodeIdToDpidCacheReverse.get(flowInfoNodeKey, 'node_not_in_topology') )

        prtLn('{} tree flows at {}'.format(getMdsalTreeType(), nodeName), 0)
        flowInfoNode = flowInfoNodes[flowInfoNodeKey]
        flowInfoTables = flowInfoNode.keys()
        flowInfoTables.sort()
        for flowInfoTable in flowInfoTables:
            for rule in flowInfoNode[flowInfoTable]:
                prtLn('{}table {}: {}'.format(spc, flowInfoTable, rule), 0)
        nodesVisited += 1

    prtLn('', 0)

# --

def getNodeBridgeIds(nodeIdFilter = None):
    resultMap = {}
    for bridge in state.bridgeNodes.values():
        if nodeIdFilter is None or nodeIdFilter in bridge.nodeId:
            resultMap[ bridge.alias ] = bridge.nodeId
    resultMapKeys = resultMap.keys()
    resultMapKeys.sort()
    return [ resultMap[x] for x in resultMapKeys ]

# --

def showPrettyBridgeNodes(indent, bridgeNodeIds, ovsdbNode = None):
    if bridgeNodeIds is None:
        return

    for nodeId in bridgeNodeIds:
        bridgeNode = state.bridgeNodes[nodeId]
        prt('{}{}:{}'.format(indent, showPrettyName(nodeId), bridgeNode.name), 0)

        if ovsdbNode is None or \
                bridgeNode.controllerTarget is None or \
                bridgeNode.controllerTarget == '' or \
                ovsdbNode.inetMgr.split(':')[0] != bridgeNode.controllerTarget.split(':')[-2] or \
                bridgeNode.controllerConnected != True:
            prt(' controller:{}'.format(bridgeNode.controllerTarget), 0)
            prt(' connected:{}'.format(bridgeNode.controllerConnected), 0)
        prtLn('', 0)
        showPrettyTerminationPoints(indent + '  ', bridgeNode.tps)

# --

def showBridgeOnlyNodes(showOrphansOnly = False):
    if len(state.bridgeNodes) == 0:
        return

    # group bridges by nodeId prefix
    resultMap = {}
    for bridge in state.bridgeNodes.values():
        nodePrefix = bridge.nodeId.split('/bridge/')[0]

        if showOrphansOnly and nodePrefix in state.ovsdbNodes:
            continue

        if nodePrefix in resultMap:
            resultMap[nodePrefix][bridge.alias] = bridge.nodeId
        else:
            resultMap[nodePrefix] = { bridge.alias: bridge.nodeId }
    resultMapKeys = resultMap.keys()
    resultMapKeys.sort()

    if len(resultMapKeys) == 0:
        return  #noop

    for nodePrefix in resultMapKeys:
        nodePrefixEntriesKeys = resultMap[nodePrefix].keys()
        nodePrefixEntriesKeys.sort()
        # prtLn('Bridges in {}: {}'.format(nodePrefix, nodePrefixEntriesKeys), 0)
        prtLn('Bridges in {}'.format(nodePrefix), 0)
        nodeIds = [ resultMap[nodePrefix][nodePrefixEntry] for nodePrefixEntry in nodePrefixEntriesKeys ]
        showPrettyBridgeNodes('  ', nodeIds)

    prtLn('', 0)

# --

def showPrettyTerminationPoints(indent, tps):

    tpsDict = {}
    for tp in tps:
        tpsDict[ tp.ofPort ] = tp

    tpDictKeys = tpsDict.keys()
    tpDictKeys.sort()
    for tpKey in tpDictKeys:
        tp = tpsDict[tpKey]
        prt('{}of:{} {}'.format(indent, tp.ofPort, tp.name), 0)
        if tp.mac != '':
            prt(' {}:{}'.format('mac', tp.mac), 0)
        if tp.ifaceId != '':
            prt(' {}:{}'.format('ifaceId', tp.ifaceId), 0)

        prtLn('', 0)

# --

def dataPathIdToOfFormat(dpId):
    return 'openflow:' + str( int('0x' + dpId.replace(':',''), 16) )

# --

def showPrettyName(name):
    if not options.useAlias:
        return name

    # handle both openflow:138604958315853:2 and openflow:138604958315853 (aka dpid)
    # also handle ovsdb://uuid/5c72ec51-1e71-4a04-ab0b-b044fb5f4dc0/bridge/br-int  (aka nodeId)
    #
    nameSplit = name.split(':')
    ofName = ':'.join(nameSplit[:2])
    ofPart = ''
    if len(nameSplit) > 2:
        ofPart = ':' + ':'.join(nameSplit[2:])

    for bridge in state.bridgeNodes.values():
        if bridge.getOpenflowName() == ofName or bridge.nodeId == name:
            return '{}{}'.format(bridge.alias, ofPart)

    # not found, return paramIn
    return name

# --

def showOfLinks():
    spc = ' ' * 2
    ofLinksKeys = state.ofLinks.keys()
    ofLinksKeys.sort()
    ofLinksKeysVisited = set()

    if len(ofLinksKeys) == 0:
        # prtLn('no ofLinks found\n', 0)
        return

    prtLn('ofLinks (discover via lldp):', 0)
    for ofLinkKey in ofLinksKeys:
        if ofLinkKey in ofLinksKeysVisited:
            continue
        if state.ofLinks.get( state.ofLinks[ofLinkKey] ) == ofLinkKey:
            prtLn('{}{} <-> {}'.format(spc, showPrettyName(ofLinkKey), showPrettyName(state.ofLinks[ofLinkKey])), 0)
            ofLinksKeysVisited.add(state.ofLinks[ofLinkKey])
        else:
            prtLn('{}{} -> {}'.format(spc, showPrettyName(ofLinkKey), showPrettyName(state.ofLinks[ofLinkKey])), 0)
        ofLinksKeysVisited.add(ofLinkKey)
    prtLn('', 0)

# --

def parseArgv():
    global options

    parser = optparse.OptionParser(version="0.1")
    parser.add_option("-d", "--debug", action="count", dest="debug", default=CONST_DEFAULT_DEBUG,
                      help="Verbosity. Can be provided multiple times for more debug.")
    parser.add_option("-n", "--noalias", action="store_false", dest="useAlias", default=True,
                      help="Do not map nodeId of bridges to an alias")
    parser.add_option("-i", "--ip", action="store", type="string", dest="odlIp", default="localhost",
                      help="opendaylights ip address")
    parser.add_option("-t", "--port", action="store", type="string", dest="odlPort", default="8080",
                      help="opendaylights listening tcp port on restconf northbound")
    parser.add_option("-u", "--user", action="store", type="string", dest="odlUsername", default="admin",
                      help="opendaylight restconf username")
    parser.add_option("-p", "--password", action="store", type="string", dest="odlPassword", default="admin",
                      help="opendaylight restconf password")
    parser.add_option("-c", "--config", action="store_true", dest="useConfigTree", default=False,
                      help="parse mdsal restconf config tree instead of operational tree")
    parser.add_option("-f", "--hide-flows", action="store_false", dest="showFlows", default=True,
                      help="hide flows")

    (options, args) = parser.parse_args(sys.argv)
    prtLn('argv options:{} args:{}'.format(options, args), 2)

# --

def doMain():
    global state

    state = State()
    parseArgv()
    buildDpidCache()
    grabTopologyJson(getMdsalTreeType())
    grabInventoryJson(getMdsalTreeType())
    parseTopologyJson(getMdsalTreeType())
    parseInventoryJson(getMdsalTreeType())
    showPrettyNamesMap()
    showNodesPretty()
    showFlowInfoPretty()
    showOfLinks()

# --

if __name__ == "__main__":
    doMain()
    sys.exit(0)
