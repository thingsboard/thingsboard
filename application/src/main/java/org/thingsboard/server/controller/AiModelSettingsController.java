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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.AI_MODEL_SETTINGS_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@Validated
@RestController
@RequestMapping("/api/ai/model/settings")
class AiModelSettingsController extends BaseController {

    @ApiOperation(
            value = "Create or update AI model settings (saveAiModelSettings)",
            notes = "Creates or updates an AI model settings record.\n\n" +
                    "• **Create:** Omit the `id` to create a new record. The platform assigns a UUID to the new settings and returns it in the `id` field of the response.\n\n" +
                    "• **Update:** Include an existing `id` to modify that record. If no matching record exists, the API responds with **404 Not Found**.\n\n" +
                    "Tenant ID for the AI model settings will be taken from the authenticated user making the request, regardless of any value provided in the request body." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping
    public AiModelSettings saveAiModelSettings(@RequestBody @Valid AiModelSettings settings) throws ThingsboardException {
        var user = getCurrentUser();
        settings.setTenantId(user.getTenantId());
        checkEntity(settings.getId(), settings, Resource.AI_MODEL_SETTINGS);
        return tbAiModelSettingsService.save(settings, user);
    }

    @ApiOperation(
            value = "Get AI model settings by ID (getAiModelSettingsById)",
            notes = "Fetches an AI model settings record by its `id`." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{settingsUuid}")
    public AiModelSettings getAiModelSettingsById(
            @Parameter(
                    description = "ID of the AI model settings record",
                    required = true,
                    example = "de7900d4-30e2-11f0-9cd2-0242ac120002"
            )
            @PathVariable UUID settingsUuid
    ) throws ThingsboardException {
        return checkAiModelSettingsId(new AiModelSettingsId(settingsUuid), Operation.READ);
    }

    @ApiOperation(
            value = "Get AI model settings (getAiModelSettings)",
            notes = "Returns a page of AI model settings. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping
    public PageData<AiModelSettings> getAiModelSettings(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = AI_MODEL_SETTINGS_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "provider", "modelId"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        var user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI_MODEL_SETTINGS, Operation.READ);
        var pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return aiModelSettingsService.findAiModelSettingsByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(
            value = "Delete AI model settings by ID (deleteAiModelSettingsById)",
            notes = "Deletes the AI model settings record by its `id`. " +
                    "If a record with the specified `id` exists, the record is deleted and the endpoint returns `true`. " +
                    "If no such record exists, the endpoint returns `false`." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{settingsUuid}")
    public boolean deleteAiModelSettingsById(
            @Parameter(
                    description = "ID of the AI model settings record",
                    required = true,
                    example = "de7900d4-30e2-11f0-9cd2-0242ac120002"
            )
            @PathVariable UUID settingsUuid
    ) throws ThingsboardException {
        var user = getCurrentUser();
        var settingsId = new AiModelSettingsId(settingsUuid);
        accessControlService.checkPermission(user, Resource.AI_MODEL_SETTINGS, Operation.DELETE);
        Optional<AiModelSettings> toDelete = aiModelSettingsService.findAiModelSettingsByTenantIdAndId(user.getTenantId(), settingsId);
        if (toDelete.isEmpty()) {
            return false;
        }
        accessControlService.checkPermission(user, Resource.AI_MODEL_SETTINGS, Operation.DELETE, settingsId, toDelete.get());
        return tbAiModelSettingsService.delete(toDelete.get(), user);
    }

}
