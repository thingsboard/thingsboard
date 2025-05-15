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

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.ai.AiSettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

// TODO: TbAiSettingsService?

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-settings")
public class AiSettingsController extends BaseController {

    private final AiSettingsService aiSettingsService;

    @PostMapping
    public AiSettings saveAiSettings(
            @RequestBody AiSettings aiSettings,

            @AuthenticationPrincipal SecurityUser requestingUser
    ) {
        return aiSettingsService.save(requestingUser.getTenantId(), aiSettings);
    }

    @GetMapping("/{aiSettingsId}")
    public AiSettings getAiSettingsById(
            @PathVariable("aiSettingsId") UUID aiSettingsUuid,

            @AuthenticationPrincipal SecurityUser requestingUser
    ) throws ThingsboardException {
        return checkNotNull(aiSettingsService.findAiSettingsByTenantIdAndId(requestingUser.getTenantId(), new AiSettingsId(aiSettingsUuid)));
    }

    @GetMapping
    public PageData<AiSettings> getAllAiSettings(
            @AuthenticationPrincipal SecurityUser requestingUser
    ) {
        return aiSettingsService.findAiSettingsByTenantId(requestingUser.getTenantId(), new PageLink(Integer.MAX_VALUE));
    }

    @DeleteMapping("/{aiSettingsId}")
    public boolean deleteAiSettingsById(
            @PathVariable("aiSettingsId") UUID aiSettingsUuid,

            @AuthenticationPrincipal SecurityUser requestingUser
    ) {
        return aiSettingsService.deleteByTenantIdAndId(requestingUser.getTenantId(), new AiSettingsId(aiSettingsUuid));
    }

}
