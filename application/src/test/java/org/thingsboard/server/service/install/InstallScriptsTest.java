/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.install.update.ImagesUpdater;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @SpyBean
    InstallScripts installScripts;

    @MockBean
    TenantService tenantService;
    @MockBean
    ApiLimitService apiLimitService;
    @SpyBean
    RuleChainDataValidator ruleChainValidator;
    @SpyBean
    ImagesUpdater imagesUpdater;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        willReturn(true).given(apiLimitService).checkEntitiesLimit(any(), any());

        when(resourceService.saveResource(any())).thenAnswer(inv -> {
            TbResource resource = inv.getArgument(0);
            if (resource.getResourceType() == ResourceType.IMAGE) {
                resource.setLink(String.format("/api/images/%s/%s",
                        resource.getTenantId().isSysTenantId() ?
                                "system" : resource.getTenantId().toString(),
                        resource.getResourceKey()
                ));
            }
            return resource;
        });
    }

    @Test
    void testDefaultRuleChainsTemplates() throws IOException {
        Path dir = installScripts.getTenantRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    @Test
    void testDefaultEdgeRuleChainsTemplates() throws IOException {
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

    @Test
    public void testWidgetsUpdate() throws Exception {
        installScripts.loadSystemWidgets();
    }

    @Test
    public void testImagesUpdater() {
        ImagesUpdater imagesUpdater = new ImagesUpdater(resourceService);
        Dashboard dashboard = new Dashboard();
        dashboard.setConfiguration(JacksonUtil.toJsonNode(dashboardConfig));
        dashboard.setTenantId(tenantId);
        imagesUpdater.updateDashboardImages(dashboard);
        System.err.println("Updated config: " + dashboard.getConfiguration().toPrettyString());
    }


    public static final String dashboardConfig = "{\n" +
            "  \"description\": \"\",\n" +
            "  \"widgets\": {\n" +
            "    \"24b26b1e-bdd0-8b2b-2a96-be150b21ae5e\": {\n" +
            "      \"typeFullFqn\": \"system.cards.value_card\",\n" +
            "      \"type\": \"latest\",\n" +
            "      \"sizeX\": 3,\n" +
            "      \"sizeY\": 3,\n" +
            "      \"config\": {\n" +
            "        \"datasources\": [\n" +
            "          {\n" +
            "            \"type\": \"device\",\n" +
            "            \"name\": \"\",\n" +
            "            \"deviceId\": \"09bfc8f0-b376-11ed-af9a-5f9d1fc4febb\",\n" +
            "            \"dataKeys\": [\n" +
            "              {\n" +
            "                \"name\": \"temperature\",\n" +
            "                \"type\": \"timeseries\",\n" +
            "                \"label\": \"Temperature\",\n" +
            "                \"color\": \"#2196f3\",\n" +
            "                \"settings\": {},\n" +
            "                \"_hash\": 0.790442736163091\n" +
            "              }\n" +
            "            ],\n" +
            "            \"alarmFilterConfig\": {\n" +
            "              \"statusList\": [\n" +
            "                \"ACTIVE\"\n" +
            "              ]\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"timewindow\": {\n" +
            "          \"displayValue\": \"\",\n" +
            "          \"selectedTab\": 0,\n" +
            "          \"realtime\": {\n" +
            "            \"realtimeType\": 1,\n" +
            "            \"interval\": 1000,\n" +
            "            \"timewindowMs\": 60000,\n" +
            "            \"quickInterval\": \"CURRENT_DAY\"\n" +
            "          },\n" +
            "          \"history\": {\n" +
            "            \"historyType\": 0,\n" +
            "            \"interval\": 1000,\n" +
            "            \"timewindowMs\": 60000,\n" +
            "            \"fixedTimewindow\": {\n" +
            "              \"startTimeMs\": 1698052629317,\n" +
            "              \"endTimeMs\": 1698139029317\n" +
            "            },\n" +
            "            \"quickInterval\": \"CURRENT_DAY\"\n" +
            "          },\n" +
            "          \"aggregation\": {\n" +
            "            \"type\": \"AVG\",\n" +
            "            \"limit\": 25000\n" +
            "          }\n" +
            "        },\n" +
            "        \"showTitle\": false,\n" +
            "        \"backgroundColor\": \"rgba(0, 0, 0, 0)\",\n" +
            "        \"color\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "        \"padding\": \"0px\",\n" +
            "        \"settings\": {\n" +
            "          \"labelPosition\": \"top\",\n" +
            "          \"layout\": \"square\",\n" +
            "          \"showLabel\": true,\n" +
            "          \"labelFont\": {\n" +
            "            \"family\": \"Roboto\",\n" +
            "            \"size\": 16,\n" +
            "            \"sizeUnit\": \"px\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"weight\": \"500\"\n" +
            "          },\n" +
            "          \"labelColor\": {\n" +
            "            \"type\": \"constant\",\n" +
            "            \"color\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "            \"colorFunction\": \"var temperature = value;\\nif (typeof temperature !== undefined) {\\n  var percent = (temperature + 60)/120 * 100;\\n  return tinycolor.mix('blue', 'red', percent).toHexString();\\n}\\nreturn 'blue';\"\n" +
            "          },\n" +
            "          \"showIcon\": true,\n" +
            "          \"iconSize\": 40,\n" +
            "          \"iconSizeUnit\": \"px\",\n" +
            "          \"icon\": \"thermostat\",\n" +
            "          \"iconColor\": {\n" +
            "            \"type\": \"constant\",\n" +
            "            \"color\": \"#5469FF\",\n" +
            "            \"colorFunction\": \"var temperature = value;\\nif (typeof temperature !== undefined) {\\n  var percent = (temperature + 60)/120 * 100;\\n  return tinycolor.mix('blue', 'red', percent).toHexString();\\n}\\nreturn 'blue';\"\n" +
            "          },\n" +
            "          \"valueFont\": {\n" +
            "            \"family\": \"Roboto\",\n" +
            "            \"size\": 52,\n" +
            "            \"sizeUnit\": \"px\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"weight\": \"500\"\n" +
            "          },\n" +
            "          \"valueColor\": {\n" +
            "            \"type\": \"constant\",\n" +
            "            \"color\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "            \"colorFunction\": \"var temperature = value;\\nif (typeof temperature !== undefined) {\\n  var percent = (temperature + 60)/120 * 100;\\n  return tinycolor.mix('blue', 'red', percent).toHexString();\\n}\\nreturn 'blue';\"\n" +
            "          },\n" +
            "          \"showDate\": true,\n" +
            "          \"dateFormat\": {\n" +
            "            \"format\": null,\n" +
            "            \"lastUpdateAgo\": true,\n" +
            "            \"custom\": false\n" +
            "          },\n" +
            "          \"dateFont\": {\n" +
            "            \"family\": \"Roboto\",\n" +
            "            \"size\": 12,\n" +
            "            \"sizeUnit\": \"px\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"weight\": \"500\"\n" +
            "          },\n" +
            "          \"dateColor\": {\n" +
            "            \"type\": \"constant\",\n" +
            "            \"color\": \"rgba(0, 0, 0, 0.38)\",\n" +
            "            \"colorFunction\": \"var temperature = value;\\nif (typeof temperature !== undefined) {\\n  var percent = (temperature + 60)/120 * 100;\\n  return tinycolor.mix('blue', 'red', percent).toHexString();\\n}\\nreturn 'blue';\"\n" +
            "          },\n" +
            "          \"background\": {\n" +
            "            \"type\": \"image\",\n" +
            "            \"imageBase64\": \"data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wCEAAQEBAQEBAUFBQUHBwYHBwoJCAgJCg8KCwoLCg8WDhAODhAOFhQYExITGBQjHBgYHCMpIiAiKTEsLDE+Oz5RUW0BBAQEBAQEBQUFBQcHBgcHCgkICAkKDwoLCgsKDxYOEA4OEA4WFBgTEhMYFCMcGBgcIykiICIpMSwsMT47PlFRbf/CABEIAtAEsAMBEQACEQEDEQH/xAA2AAEAAgIDAQEAAAAAAAAAAAAABQYEBwIDCAEJAQEAAwEBAQEAAAAAAAAAAAAAAQIDBAUGB//aAAwDAQACEAMQAAAA9/AAAAAAAXwsX42s7RHUMhntDzk8zlsOrsWZoeolQ8zbG7KF+Bf/Z\",\n" +
            "            \"imageUrl\": \"http://localhost:8080/api/resource/myimage\",\n" +
            "            \"color\": \"#fff\",\n" +
            "            \"overlay\": {\n" +
            "              \"enabled\": false,\n" +
            "              \"color\": \"rgba(255,255,255,0.72)\",\n" +
            "              \"blur\": 3\n" +
            "            }\n" +
            "          },\n" +
            "          \"autoScale\": true\n" +
            "        },\n" +
            "        \"title\": \"Value card\",\n" +
            "        \"dropShadow\": true,\n" +
            "        \"enableFullscreen\": false,\n" +
            "        \"titleStyle\": {\n" +
            "          \"fontSize\": \"16px\",\n" +
            "          \"fontWeight\": 400\n" +
            "        },\n" +
            "        \"units\": \"°C\",\n" +
            "        \"decimals\": 0,\n" +
            "        \"useDashboardTimewindow\": true,\n" +
            "        \"showLegend\": false,\n" +
            "        \"widgetStyle\": {},\n" +
            "        \"actions\": {},\n" +
            "        \"configMode\": \"basic\",\n" +
            "        \"displayTimewindow\": true,\n" +
            "        \"margin\": \"0px\",\n" +
            "        \"borderRadius\": \"0px\",\n" +
            "        \"widgetCss\": \"\",\n" +
            "        \"pageSize\": 1024,\n" +
            "        \"noDataDisplayMessage\": \"\",\n" +
            "        \"showTitleIcon\": false,\n" +
            "        \"titleTooltip\": \"\",\n" +
            "        \"titleFont\": {\n" +
            "          \"size\": 12,\n" +
            "          \"sizeUnit\": \"px\",\n" +
            "          \"family\": null,\n" +
            "          \"weight\": null,\n" +
            "          \"style\": null,\n" +
            "          \"lineHeight\": \"1.6\"\n" +
            "        },\n" +
            "        \"titleIcon\": \"\",\n" +
            "        \"iconColor\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "        \"iconSize\": \"14px\",\n" +
            "        \"timewindowStyle\": {\n" +
            "          \"showIcon\": true,\n" +
            "          \"iconSize\": \"14px\",\n" +
            "          \"icon\": \"query_builder\",\n" +
            "          \"iconPosition\": \"left\",\n" +
            "          \"font\": {\n" +
            "            \"size\": 12,\n" +
            "            \"sizeUnit\": \"px\",\n" +
            "            \"family\": null,\n" +
            "            \"weight\": null,\n" +
            "            \"style\": null,\n" +
            "            \"lineHeight\": \"1\"\n" +
            "          },\n" +
            "          \"color\": null\n" +
            "        }\n" +
            "      },\n" +
            "      \"row\": 0,\n" +
            "      \"col\": 0,\n" +
            "      \"id\": \"24b26b1e-bdd0-8b2b-2a96-be150b21ae5e\"\n" +
            "    },\n" +
            "    \"01ad2980-87c8-5813-18a2-47833d8f6df7\": {\n" +
            "      \"typeFullFqn\": \"system.date.date_range_navigator\",\n" +
            "      \"type\": \"static\",\n" +
            "      \"sizeX\": 5,\n" +
            "      \"sizeY\": 5.5,\n" +
            "      \"config\": {\n" +
            "        \"datasources\": [\n" +
            "          {\n" +
            "            \"type\": \"static\",\n" +
            "            \"name\": \"function\",\n" +
            "            \"dataKeys\": [\n" +
            "              {\n" +
            "                \"name\": \"f(x)\",\n" +
            "                \"type\": \"function\",\n" +
            "                \"label\": \"Random\",\n" +
            "                \"color\": \"#2196f3\",\n" +
            "                \"settings\": {},\n" +
            "                \"_hash\": 0.15479322438769105,\n" +
            "                \"funcBody\": \"var value = prevValue + Math.random() * 100 - 50;\\nvar multiplier = Math.pow(10, 2 || 0);\\nvar value = Math.round(value * multiplier) / multiplier;\\nif (value < -1000) {\\n\\tvalue = -1000;\\n} else if (value > 1000) {\\n\\tvalue = 1000;\\n}\\nreturn value;\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ],\n" +
            "        \"timewindow\": {\n" +
            "          \"realtime\": {\n" +
            "            \"timewindowMs\": 60000\n" +
            "          }\n" +
            "        },\n" +
            "        \"showTitle\": true,\n" +
            "        \"backgroundColor\": \"#C32F2F\",\n" +
            "        \"color\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "        \"padding\": \"8px\",\n" +
            "        \"settings\": {\n" +
            "          \"defaultInterval\": \"week\",\n" +
            "          \"stepSize\": \"day\",\n" +
            "          \"useSessionStorage\": true\n" +
            "        },\n" +
            "        \"title\": \"Date-range-navigator\",\n" +
            "        \"dropShadow\": true,\n" +
            "        \"enableFullscreen\": true,\n" +
            "        \"widgetStyle\": {},\n" +
            "        \"titleStyle\": {\n" +
            "          \"fontSize\": \"16px\",\n" +
            "          \"fontWeight\": 400\n" +
            "        },\n" +
            "        \"useDashboardTimewindow\": true,\n" +
            "        \"showLegend\": false,\n" +
            "        \"actions\": {},\n" +
            "        \"showTitleIcon\": false,\n" +
            "        \"titleTooltip\": \"\",\n" +
            "        \"widgetCss\": \"\",\n" +
            "        \"pageSize\": 1024,\n" +
            "        \"noDataDisplayMessage\": \"\"\n" +
            "      },\n" +
            "      \"row\": 0,\n" +
            "      \"col\": 0,\n" +
            "      \"id\": \"01ad2980-87c8-5813-18a2-47833d8f6df7\"\n" +
            "    },\n" +
            "    \"6dc68681-97fe-dca5-5df6-b5006797c9eb\": {\n" +
            "      \"typeFullFqn\": \"system.maps_v2.image_map\",\n" +
            "      \"type\": \"latest\",\n" +
            "      \"sizeX\": 8.5,\n" +
            "      \"sizeY\": 6.5,\n" +
            "      \"config\": {\n" +
            "        \"datasources\": [\n" +
            "          {\n" +
            "            \"type\": \"entity\",\n" +
            "            \"name\": \"\",\n" +
            "            \"entityAliasId\": \"50e40fe7-fd33-e93e-88f6-212acfa1b311\",\n" +
            "            \"filterId\": null,\n" +
            "            \"dataKeys\": [\n" +
            "              {\n" +
            "                \"name\": \"pressure\",\n" +
            "                \"type\": \"timeseries\",\n" +
            "                \"label\": \"pressure\",\n" +
            "                \"color\": \"#2196f3\",\n" +
            "                \"settings\": {},\n" +
            "                \"_hash\": 0.38936754782620264\n" +
            "              }\n" +
            "            ],\n" +
            "            \"alarmFilterConfig\": {\n" +
            "              \"statusList\": [\n" +
            "                \"ACTIVE\"\n" +
            "              ]\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"timewindow\": {\n" +
            "          \"displayValue\": \"\",\n" +
            "          \"selectedTab\": 0,\n" +
            "          \"realtime\": {\n" +
            "            \"realtimeType\": 1,\n" +
            "            \"interval\": 1000,\n" +
            "            \"timewindowMs\": 60000,\n" +
            "            \"quickInterval\": \"CURRENT_DAY\"\n" +
            "          },\n" +
            "          \"history\": {\n" +
            "            \"historyType\": 0,\n" +
            "            \"interval\": 1000,\n" +
            "            \"timewindowMs\": 60000,\n" +
            "            \"fixedTimewindow\": {\n" +
            "              \"startTimeMs\": 1698064891779,\n" +
            "              \"endTimeMs\": 1698151291779\n" +
            "            },\n" +
            "            \"quickInterval\": \"CURRENT_DAY\"\n" +
            "          },\n" +
            "          \"aggregation\": {\n" +
            "            \"type\": \"AVG\",\n" +
            "            \"limit\": 25000\n" +
            "          }\n" +
            "        },\n" +
            "        \"showTitle\": true,\n" +
            "        \"backgroundColor\": \"#fff\",\n" +
            "        \"color\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "        \"padding\": \"8px\",\n" +
            "        \"settings\": {\n" +
            "          \"provider\": \"image-map\",\n" +
            "          \"gmApiKey\": \"AIzaSyDoEx2kaGz3PxwbI9T7ccTSg5xjdw8Nw8Q\",\n" +
            "          \"gmDefaultMapType\": \"roadmap\",\n" +
            "          \"mapProvider\": \"OpenStreetMap.Mapnik\",\n" +
            "          \"useCustomProvider\": false,\n" +
            "          \"customProviderTileUrl\": \"https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png\",\n" +
            "          \"mapProviderHere\": \"HERE.normalDay\",\n" +
            "          \"credentials\": {\n" +
            "            \"useV3\": true,\n" +
            "            \"app_id\": \"AhM6TzD9ThyK78CT3ptx\",\n" +
            "            \"app_code\": \"p6NPiITB3Vv0GMUFnkLOOg\",\n" +
            "            \"apiKey\": \"kVXykxAfZ6LS4EbCTO02soFVfjA7HoBzNVVH9u7nzoE\"\n" +
            "          },\n" +
            "          \"mapImageUrl\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAyIAAAKqCAYAAADG2epfAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzs3Xd4VFX+x/H3nZ6e0ORK5CYII=\",\n" +
            "          \"tmApiKey\": \"84d6d83e0e51e481e50454ccbe8986b\",\n" +
            "          \"tmDefaultMapType\": \"roadmap\",\n" +
            "          \"latKeyName\": \"latitude\",\n" +
            "          \"lngKeyName\": \"longitude\",\n" +
            "          \"xPosKeyName\": \"xPos\",\n" +
            "          \"yPosKeyName\": \"yPos\",\n" +
            "          \"defaultCenterPosition\": \"0,0\",\n" +
            "          \"disableScrollZooming\": false,\n" +
            "          \"disableDoubleClickZooming\": false,\n" +
            "          \"disableZoomControl\": false,\n" +
            "          \"fitMapBounds\": true,\n" +
            "          \"useDefaultCenterPosition\": false,\n" +
            "          \"mapPageSize\": 16384,\n" +
            "          \"markerOffsetX\": 0.5,\n" +
            "          \"markerOffsetY\": 1,\n" +
            "          \"posFunction\": \"return {x: origXPos, y: origYPos};\",\n" +
            "          \"draggableMarker\": false,\n" +
            "          \"showLabel\": true,\n" +
            "          \"useLabelFunction\": false,\n" +
            "          \"label\": \"${entityName}\",\n" +
            "          \"showTooltip\": true,\n" +
            "          \"showTooltipAction\": \"click\",\n" +
            "          \"autocloseTooltip\": true,\n" +
            "          \"useTooltipFunction\": false,\n" +
            "          \"tooltipPattern\": \"<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}<br/><b>Temperature:</b> ${temperature} °C<br/><small>See advanced settings for details</small>\",\n" +
            "          \"tooltipOffsetX\": 0,\n" +
            "          \"tooltipOffsetY\": -1,\n" +
            "          \"color\": \"#fe7569\",\n" +
            "          \"useColorFunction\": true,\n" +
            "          \"colorFunction\": \"var type = dsData[dsIndex]['Type'];\\nif (type == 'colorpin') {\\n\\tvar temperature = dsData[dsIndex]['temperature'];\\n\\tif (typeof temperature !== undefined) {\\n\\t    var percent = (temperature + 60)/120 * 100;\\n\\t    return tinycolor.mix('blue', 'red', percent).toHexString();\\n\\t}\\n\\treturn 'blue';\\n}\\n\",\n" +
            "          \"useMarkerImageFunction\": true,\n" +
            "          \"markerImageSize\": 34,\n" +
            "          \"markerImageFunction\": \"var type = dsData[dsIndex]['Type'];\\nif (type == 'thermometer') {\\n\\tvar res = {\\n\\t    url: images[0],\\n\\t    size: 40\\n\\t}\\n\\tvar temperature = dsData[dsIndex]['temperature'];\\n\\tif (typeof temperature !== undefined) {\\n\\t    var percent = (temperature + 60)/120;\\n\\t    var index = Math.min(3, Math.floor(4 * percent));\\n\\t    res.url = images[index];\\n\\t}\\n\\treturn res;\\n}\",\n" +
            "          \"markerImages\": [\n" +
            "            \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAB/CAYAAAD4mHJdAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAACWAAAAlgB7MGOJQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAwgSURBVGiB7Zt5cBT3lce/v18fc89oRoPEIRBCHIUxp2ywCAgIxLExvoidZIFNxXE2VXHirIO3aqtSseM43qpNeZfYKecox3bhpJykYgd2w45TMcyQHIAOgBcBbAUUJI5uOM/wcaHmf3g9UM7QAAAABJRU5ErkJggg==\",\n" +
            "            \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAB/CAYAAAD4mHJdAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAACWAAAAlgB7MGOJQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAA3vSURBVGiB7Vt7cFzVef+dc+/d90OrJyO/JSOFqAtyOKzKo83MLgAkgA2AAQB+ADgCfAzjBGIsPxfh/6wbDK7xbMFYAAAAASUVORK5CYII=\",\n" +
            "            \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAB/CAYAAAD4mHJdg7EC8/8BoAc0AekgE+B/cAWpVTqSMb/AlY1WXIncMcxAAAAAElFTkSuQmCC\",\n" +
            "            \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAB/CAYAAAD4mHJdJRU5ErkJggg==\"\n" +
            "          ],\n" +
            "          \"showPolygon\": false,\n" +
            "          \"polygonKeyName\": \"perimeter\",\n" +
            "          \"editablePolygon\": false,\n" +
            "          \"showPolygonLabel\": false,\n" +
            "          \"usePolygonLabelFunction\": false,\n" +
            "          \"polygonLabel\": \"${entityName}\",\n" +
            "          \"showPolygonTooltip\": false,\n" +
            "          \"showPolygonTooltipAction\": \"click\",\n" +
            "          \"autoClosePolygonTooltip\": true,\n" +
            "          \"usePolygonTooltipFunction\": false,\n" +
            "          \"polygonTooltipPattern\": \"<b>${entityName}</b><br/><br/><b>TimeStamp:</b> ${ts:7}\",\n" +
            "          \"polygonColor\": \"#3388ff\",\n" +
            "          \"polygonOpacity\": 0.2,\n" +
            "          \"usePolygonColorFunction\": false,\n" +
            "          \"polygonStrokeColor\": \"#3388ff\",\n" +
            "          \"polygonStrokeOpacity\": 1,\n" +
            "          \"polygonStrokeWeight\": 3,\n" +
            "          \"usePolygonStrokeColorFunction\": false,\n" +
            "          \"showCircle\": false,\n" +
            "          \"circleKeyName\": \"perimeter\",\n" +
            "          \"editableCircle\": false,\n" +
            "          \"showCircleLabel\": false,\n" +
            "          \"useCircleLabelFunction\": false,\n" +
            "          \"circleLabel\": \"${entityName}\",\n" +
            "          \"showCircleTooltip\": false,\n" +
            "          \"showCircleTooltipAction\": \"click\",\n" +
            "          \"autoCloseCircleTooltip\": true,\n" +
            "          \"useCircleTooltipFunction\": false,\n" +
            "          \"circleTooltipPattern\": \"<b>${entityName}</b><br/><br/><b>TimeStamp:</b> ${ts:7}\",\n" +
            "          \"circleFillColor\": \"#3388ff\",\n" +
            "          \"circleFillColorOpacity\": 0.2,\n" +
            "          \"useCircleFillColorFunction\": false,\n" +
            "          \"circleStrokeColor\": \"#3388ff\",\n" +
            "          \"circleStrokeOpacity\": 1,\n" +
            "          \"circleStrokeWeight\": 3,\n" +
            "          \"useCircleStrokeColorFunction\": false\n" +
            "        },\n" +
            "        \"title\": \"Image Map\",\n" +
            "        \"dropShadow\": true,\n" +
            "        \"enableFullscreen\": true,\n" +
            "        \"titleStyle\": {\n" +
            "          \"fontSize\": \"16px\",\n" +
            "          \"fontWeight\": 400\n" +
            "        },\n" +
            "        \"useDashboardTimewindow\": true,\n" +
            "        \"showLegend\": false,\n" +
            "        \"widgetStyle\": {},\n" +
            "        \"actions\": {},\n" +
            "        \"displayTimewindow\": true\n" +
            "      },\n" +
            "      \"row\": 0,\n" +
            "      \"col\": 0,\n" +
            "      \"id\": \"6dc68681-97fe-dca5-5df6-b5006797c9eb\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"states\": {\n" +
            "    \"default\": {\n" +
            "      \"name\": \"Images\",\n" +
            "      \"root\": true,\n" +
            "      \"layouts\": {\n" +
            "        \"main\": {\n" +
            "          \"widgets\": {\n" +
            "            \"24b26b1e-bdd0-8b2b-2a96-be150b21ae5e\": {\n" +
            "              \"sizeX\": 6,\n" +
            "              \"sizeY\": 5,\n" +
            "              \"row\": 0,\n" +
            "              \"col\": 18\n" +
            "            },\n" +
            "            \"01ad2980-87c8-5813-18a2-47833d8f6df7\": {\n" +
            "              \"sizeX\": 5,\n" +
            "              \"sizeY\": 5,\n" +
            "              \"row\": 0,\n" +
            "              \"col\": 13\n" +
            "            },\n" +
            "            \"6dc68681-97fe-dca5-5df6-b5006797c9eb\": {\n" +
            "              \"sizeX\": 8,\n" +
            "              \"sizeY\": 6,\n" +
            "              \"row\": 6,\n" +
            "              \"col\": 13\n" +
            "            }\n" +
            "          },\n" +
            "          \"gridSettings\": {\n" +
            "            \"backgroundColor\": \"#eeeeee\",\n" +
            "            \"columns\": 24,\n" +
            "            \"margin\": 10,\n" +
            "            \"outerMargin\": true,\n" +
            "            \"backgroundSizeMode\": \"auto\",\n" +
            "            \"autoFillHeight\": false,\n" +
            "            \"backgroundImageUrl\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAyIAAAKqCAYAAADG2epfAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzscvuw4MRvxaz5gVL8owhtF0n7gs8zcmnLxWDL3sejVwbSPaER4uzt4fl4saeVj6pOIiIjcIP4fn4t1aCx7Vb0AAAAASUVORK5CYII=\",\n" +
            "            \"mobileAutoFillHeight\": false,\n" +
            "            \"mobileRowHeight\": 70\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entityAliases\": {\n" +
            "    \"50e40fe7-fd33-e93e-88f6-212acfa1b311\": {\n" +
            "      \"id\": \"50e40fe7-fd33-e93e-88f6-212acfa1b311\",\n" +
            "      \"alias\": \"aa\",\n" +
            "      \"filter\": {\n" +
            "        \"type\": \"entityList\",\n" +
            "        \"resolveMultiple\": true,\n" +
            "        \"entityType\": \"DEVICE\",\n" +
            "        \"entityList\": [\n" +
            "          \"5a6c89e0-5ea9-11ed-8ee5-3570b5c0f66a\"\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"filters\": {},\n" +
            "  \"timewindow\": {\n" +
            "    \"hideInterval\": false,\n" +
            "    \"hideLastInterval\": false,\n" +
            "    \"hideQuickInterval\": false,\n" +
            "    \"hideAggregation\": false,\n" +
            "    \"hideAggInterval\": false,\n" +
            "    \"hideTimezone\": false,\n" +
            "    \"selectedTab\": 1,\n" +
            "    \"history\": {\n" +
            "      \"historyType\": 1,\n" +
            "      \"fixedTimewindow\": {\n" +
            "        \"startTimeMs\": 1697576400000,\n" +
            "        \"endTimeMs\": 1698181199999\n" +
            "      },\n" +
            "      \"interval\": 1210000\n" +
            "    },\n" +
            "    \"aggregation\": {\n" +
            "      \"type\": \"AVG\",\n" +
            "      \"limit\": 25000\n" +
            "    }\n" +
            "  },\n" +
            "  \"settings\": {\n" +
            "    \"stateControllerId\": \"entity\",\n" +
            "    \"showTitle\": false,\n" +
            "    \"showDashboardsSelect\": true,\n" +
            "    \"showEntitiesSelect\": true,\n" +
            "    \"showDashboardTimewindow\": true,\n" +
            "    \"showDashboardExport\": true,\n" +
            "    \"toolbarAlwaysOpen\": true,\n" +
            "    \"titleColor\": \"rgba(0,0,0,0.870588)\",\n" +
            "    \"showDashboardLogo\": true,\n" +
            "    \"dashboardLogoUrl\": \"data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wCEAAQEBAQEBAUFBQUHBwYHBwoJCAgJCg8KCwoLCg8WDhAODhAOFhQYExITGBQjHBgYHCMpIiAiKTEsLDE+Oz5RlQ8zbG7KF+Bf/Z\",\n" +
            "    \"hideToolbar\": false,\n" +
            "    \"showFilters\": true,\n" +
            "    \"showUpdateDashboardImage\": true,\n" +
            "    \"dashboardCss\": \"\"\n" +
            "  }\n" +
            "}";

}
