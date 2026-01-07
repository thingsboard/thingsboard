/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.install.update.ResourcesUpdater;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;

@Slf4j
@SpringBootTest(classes = {InstallScripts.class, RuleChainDataValidator.class})
class InstallScriptsTest {

    @MockBean
    RuleChainService ruleChainService;
    @MockBean
    DashboardService dashboardService;
    @MockBean
    WidgetTypeService widgetTypeService;
    @MockBean
    WidgetsBundleService widgetsBundleService;
    @MockBean
    OAuth2ConfigTemplateService oAuth2TemplateService;
    @MockBean
    ResourceService resourceService;
    @MockBean
    ImageService imageService;
    @MockBean
    ResourcesUpdater resourcesUpdater;
    @SpyBean
    InstallScripts installScripts;

    @MockBean
    TenantService tenantService;
    @MockBean
    ApiLimitService apiLimitService;
    @SpyBean
    RuleChainDataValidator ruleChainValidator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        willReturn(true).given(apiLimitService).checkEntitiesLimit(any(), any());
    }

    @Test
    void testDefaultRuleChainsTemplates() {
        Path dir = installScripts.getTenantRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    @Test
    void testDefaultEdgeRuleChainsTemplates() {
        Path dir = installScripts.getEdgeRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    @Test
    void testDeviceProfileDefaultRuleChainTemplate() {
        validateRuleChainTemplate(installScripts.getDeviceProfileDefaultRuleChainTemplateFilePath());
    }

    private void validateRuleChainTemplate(Path templateFilePath) {
        log.warn("validateRuleChainTemplate {}", templateFilePath);
        JsonNode ruleChainJson = JacksonUtil.toJsonNode(templateFilePath.toFile());

        RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
        ruleChain.setTenantId(tenantId);
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        ruleChain.setId(new RuleChainId(UUID.randomUUID()));

        RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        List<Throwable> throwables = RuleChainDataValidator.validateMetaData(ruleChainMetaData);

        assertThat(throwables).as("templateFilePath " + templateFilePath)
                .containsExactlyInAnyOrderElementsOf(Collections.emptyList());
    }

}
