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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

@AllArgsConstructor
@Slf4j
public class AdminSettingsEdgeEventFetcher extends BasePageableEdgeEventFetcher {

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        return null;
    }


//
//    private void syncAdminSettings(TenantId tenantId, Edge edge) {
//        log.trace("[{}] syncAdminSettings [{}]", tenantId, edge.getName());
//        try {
//            AdminSettings systemMailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(systemMailSettings));
//            AdminSettings tenantMailSettings = convertToTenantAdminSettings(systemMailSettings.getKey(), (ObjectNode) systemMailSettings.getJsonValue());
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(tenantMailSettings));
//            AdminSettings systemMailTemplates = loadMailTemplates();
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(systemMailTemplates));
//            AdminSettings tenantMailTemplates = convertToTenantAdminSettings(systemMailTemplates.getKey(), (ObjectNode) systemMailTemplates.getJsonValue());
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(tenantMailTemplates));
//        } catch (Exception e) {
//            log.error("Can't load admin settings", e);
//        }
//    }
//
//    private AdminSettings loadMailTemplates() throws Exception {
//        Map<String, Object> mailTemplates = new HashMap<>();
//        Pattern startPattern = Pattern.compile("<div class=\"content\".*?>");
//        Pattern endPattern = Pattern.compile("<div class=\"footer\".*?>");
//        File[] files = new DefaultResourceLoader().getResource("classpath:/templates/").getFile().listFiles();
//        for (File file : files) {
//            Map<String, String> mailTemplate = new HashMap<>();
//            String name = validateName(file.getName());
//            String stringTemplate = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//            Matcher start = startPattern.matcher(stringTemplate);
//            Matcher end = endPattern.matcher(stringTemplate);
//            if (start.find() && end.find()) {
//                String body = StringUtils.substringBetween(stringTemplate, start.group(), end.group()).replaceAll("\t", "");
//                String subject = StringUtils.substringBetween(body, "<h2>", "</h2>");
//                mailTemplate.put("subject", subject);
//                mailTemplate.put("body", body);
//                mailTemplates.put(name, mailTemplate);
//            } else {
//                log.error("Can't load mail template from file {}", file.getName());
//            }
//        }
//        AdminSettings adminSettings = new AdminSettings();
//        adminSettings.setId(new AdminSettingsId(Uuids.timeBased()));
//        adminSettings.setKey("mailTemplates");
//        adminSettings.setJsonValue(mapper.convertValue(mailTemplates, JsonNode.class));
//        return adminSettings;
//    }
//
//    private String validateName(String name) throws Exception {
//        StringBuilder nameBuilder = new StringBuilder();
//        name = name.replace(".vm", "");
//        String[] nameParts = name.split("\\.");
//        if (nameParts.length >= 1) {
//            nameBuilder.append(nameParts[0]);
//            for (int i = 1; i < nameParts.length; i++) {
//                String word = WordUtils.capitalize(nameParts[i]);
//                nameBuilder.append(word);
//            }
//            return nameBuilder.toString();
//        } else {
//            throw new Exception("Error during filename validation");
//        }
//    }
//
//    private AdminSettings convertToTenantAdminSettings(String key, ObjectNode jsonValue) {
//        AdminSettings tenantMailSettings = new AdminSettings();
//        jsonValue.put("useSystemMailSettings", true);
//        tenantMailSettings.setJsonValue(jsonValue);
//        tenantMailSettings.setKey(key);
//        return tenantMailSettings;
//    }
}
