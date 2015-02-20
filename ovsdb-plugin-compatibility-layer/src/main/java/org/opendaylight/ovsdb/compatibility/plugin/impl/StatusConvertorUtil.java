/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.compatibility.plugin.api.StatusWithUuid;

/**
 * Class providers convenience methods for converting *.plugin.api.Status object to *.sal.utils.Status object.
 *
 * @author Anil Vishnoi ( vishnoianil@gmail.com)
 *
 */
public class StatusConvertorUtil {

    public static StatusWithUuid convertOvsdbStatusWithUuidToCompLayerStatusWithUuid(org.opendaylight.ovsdb.plugin.api.StatusWithUuid statusWithUuid){
        if(statusWithUuid.getUuid() != null){
            return new StatusWithUuid(convertOvsdbStatusCodeToSalStatusCode(statusWithUuid.getCode()),statusWithUuid.getUuid());
        }else if(statusWithUuid.getRequestId() != 0){
            return new StatusWithUuid(convertOvsdbStatusCodeToSalStatusCode(statusWithUuid.getCode()),statusWithUuid.getRequestId());
        }else{
            return new StatusWithUuid(convertOvsdbStatusCodeToSalStatusCode(statusWithUuid.getCode()),statusWithUuid.getDescription());
        }

    }

    public static Status convertOvsdbStatusToSalStatus(org.opendaylight.ovsdb.plugin.api.Status status){
        if(status.getRequestId() != 0){
            return new org.opendaylight.controller.sal.utils.Status(convertOvsdbStatusCodeToSalStatusCode(status.getCode()),status.getRequestId());
        }else{
            return new org.opendaylight.controller.sal.utils.Status(convertOvsdbStatusCodeToSalStatusCode(status.getCode()),status.getDescription());
        }
    }

    private static StatusCode convertOvsdbStatusCodeToSalStatusCode(org.opendaylight.ovsdb.plugin.api.StatusCode statusCode){
        switch(statusCode){
        case SUCCESS:
            return StatusCode.SUCCESS;
        case CREATED:
            return StatusCode.CREATED;
        case BADREQUEST:
            return StatusCode.BADREQUEST;
        case UNAUTHORIZED:
            return StatusCode.UNAUTHORIZED;
        case FORBIDDEN:
            return StatusCode.FORBIDDEN;
        case NOTFOUND:
            return StatusCode.NOTFOUND;
        case NOTALLOWED:
            return StatusCode.NOTALLOWED;
        case NOTACCEPTABLE:
            return StatusCode.NOTACCEPTABLE;
        case TIMEOUT:
            return StatusCode.TIMEOUT;
        case CONFLICT:
            return StatusCode.CONFLICT;
        case GONE:
            return StatusCode.GONE;
        case UNSUPPORTED:
            return StatusCode.UNSUPPORTED;

        case INTERNALERROR:
            return StatusCode.INTERNALERROR;
        case NOTIMPLEMENTED:
            return StatusCode.NOTIMPLEMENTED;
        case NOSERVICE:
            return StatusCode.NOSERVICE;
        case UNDEFINED:
            return StatusCode.UNDEFINED;
        default:
            return StatusCode.UNSUPPORTED;
        }
    }

}
