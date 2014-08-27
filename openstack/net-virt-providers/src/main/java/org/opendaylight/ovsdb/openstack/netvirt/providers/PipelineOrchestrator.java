/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers;

import java.util.UUID;

/**
 * A PipelineOrchestrator provides the necessary orchestration logic to allow multiple network services
 * to share a common OpenFlow 1.3 based multi-table pipeline.
 */
public interface PipelineOrchestrator {

  enum ServiceDirection {
    UNDEFINED,
    INGRESS,
    EGRESS
  }

  static final Integer PRE_NAT = 2;
  static final Integer PRE_ACL = 7;
  //ToDo: Add some sensible altitude constants

  /**
   * Register a new service in the pipeline
   * @param altitude an integer representing the desired placement in the pipeline where 0 = lowest.
   * @param direction whether the service is ingress, egress
   * @return a unique service identifier
   */
  UUID registerService(Integer altitude, ServiceDirection direction);

  /**
   * Unregister a service from the pipeline
   * This clears any assigned table IDs and registers
   */
  void unregisterService(UUID serviceID);

  /**
   * Get the OpenFlow Table ID
   * @param serviceID unique service identifier
   */
  Integer getTableId(UUID serviceID);

  /**
   * Assign an OVS Register to a service
   * @return the integer value of the assigned register
   */
  Integer assignRegister(UUID serviceID);

}
