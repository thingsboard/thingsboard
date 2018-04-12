/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.controller.plugin;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.extensions.api.plugins.PluginConstants;

@RestController
@RequestMapping(PluginConstants.PLUGIN_URL_PREFIX)
@Slf4j
public class PluginApiController extends BaseController {

//    @SuppressWarnings("rawtypes")
//    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
//    @RequestMapping(value = "/{pluginToken}/**")
//    @ResponseStatus(value = HttpStatus.OK)
//    public DeferredResult<ResponseEntity> processRequest(
//            @PathVariable("pluginToken") String pluginToken,
//            RequestEntity<byte[]> requestEntity,
//            HttpServletRequest request)
//            throws ThingsboardException {
//        log.debug("[{}] Going to process requst uri: {}", pluginToken, requestEntity.getUrl());
//        DeferredResult<ResponseEntity> result = new DeferredResult<ResponseEntity>();
//        PluginMetaData pluginMd = pluginService.findPluginByApiToken(pluginToken);
//        if (pluginMd == null) {
//            result.setErrorResult(new PluginNotFoundException("Plugin with token: " + pluginToken + " not found!"));
//        } else {
//            TenantId tenantId = getCurrentUser().getTenantId();
//            CustomerId customerId = getCurrentUser().getCustomerId();
//            if (validatePluginAccess(pluginMd, tenantId, customerId)) {
//                if(tenantId != null && ModelConstants.NULL_UUID.equals(tenantId.getId())){
//                    tenantId = null;
//                }
//                UserId userId = getCurrentUser().getId();
//                String userName = getCurrentUser().getName();
//                PluginApiCallSecurityContext securityCtx = new PluginApiCallSecurityContext(pluginMd.getTenantId(), pluginMd.getId(),
//                        tenantId, customerId, userId, userName);
//                actorService.process(new BasicPluginRestMsg(securityCtx, new RestRequest(requestEntity, request), result));
//            } else {
//                result.setResult(new ResponseEntity<>(HttpStatus.FORBIDDEN));
//            }
//
//        }
//        return result;
//    }
//
//    public static boolean validatePluginAccess(PluginMetaData pluginMd, TenantId tenantId, CustomerId customerId) {
//        boolean systemAdministrator = tenantId == null || ModelConstants.NULL_UUID.equals(tenantId.getId());
//        boolean tenantAdministrator = !systemAdministrator && (customerId == null || ModelConstants.NULL_UUID.equals(customerId.getId()));
//        boolean systemPlugin = ModelConstants.NULL_UUID.equals(pluginMd.getTenantId().getId());
//
//        boolean validUser = false;
//        if (systemPlugin) {
//            if (pluginMd.isPublicAccess() || systemAdministrator) {
//                // All users can access public system plugins. Only system
//                // users can access private system plugins
//                validUser = true;
//            }
//        } else {
//            if ((pluginMd.isPublicAccess() || tenantAdministrator) && tenantId != null && tenantId.equals(pluginMd.getTenantId())) {
//                // All tenant users can access public tenant plugins. Only tenant
//                // administrator can access private tenant plugins
//                validUser = true;
//            }
//        }
//        return validUser;
//    }
}
