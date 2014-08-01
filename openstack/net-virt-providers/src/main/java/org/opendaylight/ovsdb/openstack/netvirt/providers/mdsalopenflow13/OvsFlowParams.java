/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removed this to troubleshoot Node problems.
 */
public class OvsFlowParams {
    static final Logger logger = LoggerFactory.getLogger(OvsFlowParams.class);
    private Integer hardTimeout;
    private FlowModFlags flowModFlags;
    private String flowId;
    private FlowKey flowKey;
    private String flowName;
    private FlowCookie flowCookie;
    private FlowCookie flowCookieMask;
    private Boolean installHW;
    private Boolean strict;
    private Boolean barrier;
    private Integer priority;
    private Integer idleTimeout;
    private String containerName;
    private Short writeTableId;
    private FlowBuilder flowBuilder;
    private OvsFlowParams ovsFlowParams;
    public Boolean barrier() { return this.barrier; }
    public Integer hardTimeout() { return this.hardTimeout; }
    public FlowModFlags flowModFlags() { return this.flowModFlags; }
    public String flowId() { return this.flowId; }
    public FlowKey flowKey() { return this.flowKey; }
    public String flowName() { return this.flowName; }
    public FlowCookie flowCookie() { return this.flowCookie; }
    public FlowCookie flowCookieMask() { return this.flowCookieMask; }
    public Boolean installHW() { return this.installHW; }
    public Boolean strict() { return this.strict; }
    public Integer priority() {return this.priority; }
    public Integer idleTimeout() { return this.idleTimeout; }
    public String containerName() { return this.containerName; }
    public Short writeTableId() { return this.writeTableId; }
    /**
     * Hard timeout.
     *
     * @param hardTimeout the hard timeout
     * @return the ovs flow params
     */
    public OvsFlowParams hardTimeout(final Integer hardTimeout) {
        this.hardTimeout = hardTimeout;
        return this;
    }
    /**
     * Barrier ovs flow params.
     *
     * @param barrier the barrier
     * @return the ovs flow params
     */
    public OvsFlowParams barrier(final Boolean barrier) {
        this.barrier = barrier;
        return this;
    }
    /**
     * Flow mod flags.
     *
     * @param flowModFlags the flow mod flags
     * @return the ovs flow params
     */
    public OvsFlowParams flowModFlags(
            final FlowModFlags flowModFlags) {
        this.flowModFlags = flowModFlags;
        return this;
    }
    /**
     * Flow id.
     *
     * @param flowId the flow id
     * @return the ovs flow params
     */
    public OvsFlowParams flowId(final String flowId) {
        this.flowId = flowId;
        return this;
    }
    /**
     * Flow key.
     *
     * @param flowKey the flow key
     * @return the ovs flow params
     */
    public OvsFlowParams flowKey(
            final FlowKey flowKey) {
        this.flowKey = flowKey;
        return this;
    }
    /**
     * Flow name.
     *
     * @param flowName the flow name
     * @return the ovs flow params
     */
    public OvsFlowParams flowName(final String flowName) {
        this.flowName = flowName;
        return this;
    }
    /**
     * Flow cookie.
     *
     * @param flowCookie the flow cookie
     * @return the ovs flow params
     */
    public OvsFlowParams flowCookie(
            final FlowCookie flowCookie) {
        this.flowCookie = flowCookie;
        return this;
    }
    /**
     * Flow cookie mask.
     *
     * @param flowCookieMask the flow cookie mask
     * @return the ovs flow params
     */
    public OvsFlowParams flowCookieMask(
            final FlowCookie flowCookieMask) {
        this.flowCookieMask = flowCookieMask;
        return this;
    }
    /**
     * Install hW.
     *
     * @param installHW the install hW
     * @return the ovs flow params
     */
    public OvsFlowParams installHW(final Boolean installHW) {
        this.installHW = installHW;
        return this;
    }
    /**
     * Strict ovs flow params.
     *
     * @param strict the strict
     * @return the ovs flow params
     */
    public OvsFlowParams strict(final Boolean strict) {
        this.strict = strict;
        return this;
    }
    /**
     * Priority ovs flow params.
     *
     * @param priority the priority
     * @return the ovs flow params
     */
    public OvsFlowParams priority(final Integer priority) {
        this.priority = priority;
        return this;
    }
    /**
     * Idle timeout.
     *
     * @param idleTimeout the idle timeout
     * @return the ovs flow params
     */
    public OvsFlowParams idleTimeout(final Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }
    /**
     * Container name.
     *
     * @param containerName the container name
     * @return the ovs flow params
     */
    public OvsFlowParams containerName(final String containerName) {
        this.containerName = containerName;
        return this;
    }
    /**
     * Write table id.
     *
     * @param writeTableId the write table id
     * @return the ovs flow params
     */
    public OvsFlowParams writeTableId(final Short writeTableId) {
        this.writeTableId = writeTableId;
        return this;
    }

    public void buildOtherProviderOpenFlowImplementationsHere() {
        // TODO Other Providers and Versions are simply new builders in this class
    }
    /**
     * Build flow params.
     *
     * @param flowBuilder the flow builder
     * @param ovsFlowParams the ovs flow params
     * @return the flow builder
     */
    public FlowBuilder buildMDSalFlowParams(FlowBuilder flowBuilder, OvsFlowParams ovsFlowParams) {
        this.flowBuilder = flowBuilder;
        this.ovsFlowParams = ovsFlowParams;
        if (this.priority() != null) {
            flowBuilder.setPriority(ovsFlowParams.priority());
        }
        if (this.flowName() != null) {
            flowBuilder.setFlowName(ovsFlowParams.flowName());
            flowBuilder.setId(new FlowId(ovsFlowParams.flowName()));
            FlowKey key = new FlowKey(new FlowId(ovsFlowParams.flowName()));
            flowBuilder.setKey(key);
        }
        if (this.idleTimeout() != null) {
            flowBuilder.setIdleTimeout(idleTimeout);
        } else flowBuilder.setIdleTimeout(0);

        if (this.hardTimeout() != null) {
            flowBuilder.setHardTimeout(hardTimeout);
        } else flowBuilder.setHardTimeout(0);
        if (this.strict() != null) {
            flowBuilder.setStrict(Boolean.TRUE);
        }
        if (this.barrier() != null) {
            flowBuilder.setBarrier(Boolean.TRUE);
        }
        if (this.containerName() != null) {
            flowBuilder.setContainerName(containerName);
        }
        if (this.flowCookie() != null) {
            flowBuilder.setCookie(new FlowCookie(flowCookie));
        }
        if (this.flowCookieMask() != null) {
            flowBuilder.setCookie(new FlowCookie(flowCookieMask));
        }
        if (this.writeTableId() != null) {
            flowBuilder.setTableId(ovsFlowParams.writeTableId());
        }
        return flowBuilder;
    }

    @Override
    public String toString() {
        return "OvsFlowParams{" +
                "hardTimeout=" + hardTimeout +
                ", flowModFlags=" + flowModFlags +
                ", flowId='" + flowId + '\'' +
                ", flowKey=" + flowKey +
                ", flowName='" + flowName + '\'' +
                ", flowCookie=" + flowCookie +
                ", flowCookieMask=" + flowCookieMask +
                ", installHW=" + installHW +
                ", strict=" + strict +
                ", barrier=" + barrier +
                ", priority=" + priority +
                ", idleTimeout=" + idleTimeout +
                ", containerName='" + containerName + '\'' +
                ", writeTableId=" + writeTableId +
                '}';
    }
}