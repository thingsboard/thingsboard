/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.RPC_V2_URL_PREFIX)
@Slf4j
public class RpcV2Controller extends AbstractRpcController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/oneway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleOneWayDeviceRPCRequest(@PathVariable("deviceId") String deviceIdStr, @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(true, new DeviceId(UUID.fromString(deviceIdStr)), requestBody, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/twoway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleTwoWayDeviceRPCRequest(@PathVariable("deviceId") String deviceIdStr, @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(false, new DeviceId(UUID.fromString(deviceIdStr)), requestBody, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/{rpcId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Rpc> getPersistedRpc(@PathVariable("rpcId") String strRpc) throws ThingsboardException {
        checkParameter("RpcId", strRpc);
        try {
            RpcId rpcId = new RpcId(UUID.fromString(strRpc));
            Rpc rpc = checkRpcId(rpcId, Operation.READ);
            HttpStatus status;
            switch (rpc.getStatus()) {
                case FAILED:
                    status = HttpStatus.BAD_GATEWAY;
                    break;
                case TIMEOUT:
                    status = HttpStatus.GATEWAY_TIMEOUT;
                    break;
                case QUEUED:
                case DELIVERED:
                    status = HttpStatus.ACCEPTED;
                    break;
                default:
                    status = HttpStatus.OK;
            }
            return new ResponseEntity<>(rpc, status);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<Rpc> getPersistedRpcByDevice(@PathVariable("deviceId") String strDeviceId,
                                                 @RequestParam int pageSize,
                                                 @RequestParam int page,
                                                 @RequestParam RpcStatus rpcStatus,
                                                 @RequestParam(required = false) String textSearch,
                                                 @RequestParam(required = false) String sortProperty,
                                                 @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("DeviceId", strDeviceId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            DeviceId deviceId = new DeviceId(UUID.fromString(strDeviceId));
            return checkNotNull(rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/persistent/{rpcId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteResource(@PathVariable("rpcId") String strRpc) throws ThingsboardException {
        checkParameter("RpcId", strRpc);
        try {
            rpcService.deleteRpc(getTenantId(), new RpcId(UUID.fromString(strRpc)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
