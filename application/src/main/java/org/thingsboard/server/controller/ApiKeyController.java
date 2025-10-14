/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.API_KEY_HEADER_PREFIX;
import static org.thingsboard.server.controller.ControllerConstants.API_KEY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiKeyController extends BaseController {

    private final ApiKeyService apiKeyService;

    @ApiOperation(value = "Save API key for user (saveApiKey)",
            notes = "Creates an API key for the given user and returns the token ONCE as 'ApiKey <hash>'." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @PostMapping(value = "/apiKey")
    public ApiKey saveApiKey(
            @Parameter(description = "A JSON value representing the Api Key token.")
            @RequestBody @Valid ApiKeyInfo apiKeyInfo) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        apiKeyInfo.setTenantId(securityUser.getTenantId());
        checkEntity(apiKeyInfo.getId(), apiKeyInfo, Resource.API_KEY);
        ApiKey savedApiKey = checkNotNull(apiKeyService.saveApiKey(securityUser.getTenantId(), apiKeyInfo));
        savedApiKey.setValue(toUserApiKey(savedApiKey.getValue()));
        return savedApiKey;
    }

    @ApiOperation(value = "Get User Api Keys (getUserApiKeys)",
            notes = "Returns a page of api keys owned by user. " +
                    PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @GetMapping(value = "/apiKeys/{userId}")
    public PageData<ApiKeyInfo> getUserApiKeys(
            @Parameter(description = USER_ID_PARAM_DESCRIPTION)
            @PathVariable("userId") String userIdStr,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        PageLink pageLink = createPageLink(pageSize, page, null, null, null);
        UserId userId = new UserId(toUUID(userIdStr));
        accessControlService.checkPermission(securityUser, Resource.API_KEY, Operation.READ);
        return apiKeyService.findApiKeysByUserId(securityUser.getTenantId(), userId, pageLink);
    }

    @ApiOperation(value = "Update API key Description",
            notes = "Updates the description of the existing API key by apiKeyId. " +
                    "Only the description can be updated. " +
                    "Referencing a non-existing ApiKey Id will cause a 'Not Found' error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @PutMapping("/apiKey/{id}/description")
    public ApiKeyInfo updateApiKeyDescription(
            @Parameter(description = API_KEY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable UUID id,
            @Parameter(description = "New description for the API key", example = "Description")
            @RequestBody Optional<String> description) throws Exception {
        ApiKeyId apiKeyId = new ApiKeyId(id);
        ApiKey apiKey = checkApiKeyId(apiKeyId, Operation.WRITE);
        apiKey.setDescription(description.orElse(null));
        return apiKeyService.saveApiKey(apiKey.getTenantId(), apiKey);
    }

    @ApiOperation(value = "Enable or disable API key (enableApiKey)",
            notes = "Updates api key with enabled = true/false. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @PutMapping(value = "/apiKey/{id}/enabled/{enabledValue}")
    public ApiKeyInfo enableApiKey(
            @Parameter(description = "Unique identifier of the API key to enable/disable", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Enabled or disabled api key", required = true)
            @PathVariable(value = "enabledValue") Boolean enabledValue) throws ThingsboardException {
        ApiKeyId apiKeyId = new ApiKeyId(id);
        ApiKey apiKey = checkApiKeyId(apiKeyId, Operation.WRITE);
        apiKey.setEnabled(enabledValue);
        return apiKeyService.saveApiKey(apiKey.getTenantId(), apiKey);
    }

    @ApiOperation(value = "Delete API key by ID (deleteApiKey)",
            notes = "Deletes the API key. Referencing non-existing ApiKey Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @DeleteMapping(value = "/apiKey/{id}")
    public void deleteApiKey(@PathVariable UUID id) throws ThingsboardException {
        ApiKeyId apiKeyId = new ApiKeyId(id);
        ApiKey apiKey = checkApiKeyId(apiKeyId, Operation.DELETE);
        apiKeyService.deleteApiKey(apiKey.getTenantId(), apiKey, false);
    }

    private String toUserApiKey(String value) {
        return API_KEY_HEADER_PREFIX + value;
    }

}
