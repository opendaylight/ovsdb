#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# SPDX-License-Identifier: EPL-1.0
##############################################################################
# Copyright (c) 2018 The Linux Foundation and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
##############################################################################

from docs_conf.conf import *

# Append to intersphinx_mapping
#intersphinx_mapping['odl-releng-builder'] = ('http://docs.opendaylight.org/projects/releng-builder/en/latest/', None)

linkcheck_ignore = [
    # Ignore jenkins because it's often slow to respond.
    'https://jenkins.opendaylight.org/releng',
    'https://jenkins.opendaylight.org/sandbox',
    # ponytail: permanently dead/example links; drop a pattern if a host returns.
    r'http://odl:8181/.*',                       # example REST endpoint, not resolvable
    r'https://docs\.google\.com/presentation/.*',  # slide anchors never resolve
    r'https://git\.opendaylight\.org/gerrit/#/.*',  # retired gerrit anchor URLs
    r'https://wiki\.opendaylight\.org/.*',       # wiki migrated to atlassian, 404s
    r'https://github\.com/opendaylight/yangtools/blob/stable/boron/.*',  # deleted boron file
    r'http://(www\.)?openvswitch\.org/docs/.*',  # moved OVS docs, 404
]

nitpicky = True
