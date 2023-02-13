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
package org.thingsboard.server.dao.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.NodeType;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserSettings;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("UserSettingsDaoService")
@Slf4j
@RequiredArgsConstructor
public class UserSettingsServiceImpl extends AbstractCachedService<UserId, UserSettings, UserSettingsEvictEvent> implements UserSettingsService {
    public static final String INCORRECT_USER_ID = "Incorrect userId ";
    private final UserSettingsDao userSettingsDao;

    @Override
    public UserSettings saveUserSettings(TenantId tenantId, UserSettings userSettings) {
        log.trace("Executing saveUserSettings for user [{}], [{}]", userSettings.getUserId(), userSettings);
        validateId(userSettings.getUserId(), INCORRECT_USER_ID + userSettings.getUserId());
        return doSaveUserSettings(tenantId, userSettings);
    }

    @Override
    public void updateUserSettings(TenantId tenantId, UserId userId, JsonNode settings) {
        log.trace("Executing updateUserSettings for user [{}], [{}]", userId, settings);
        validateId(userId, INCORRECT_USER_ID + userId);

        UserSettings oldSettings = userSettingsDao.findById(tenantId, userId);
        JsonNode oldSettingsJson = oldSettings != null ? oldSettings.getSettings() : JacksonUtil.newObjectNode();

        UserSettings newUserSettings = new UserSettings();
        newUserSettings.setUserId(userId);
        newUserSettings.setSettings(update(oldSettingsJson, settings));
        doSaveUserSettings(tenantId, newUserSettings);
    }

    @Override
    public UserSettings findUserSettings(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserSettings for user [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);

        return cache.getAndPutInTransaction(userId,
                () -> userSettingsDao.findById(tenantId, userId), true);
    }

    @Override
    public void deleteUserSettings(TenantId tenantId, UserId userId, List<String> jsonPaths) {
        log.trace("Executing deleteUserSettings for user [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        UserSettings userSettings = userSettingsDao.findById(tenantId, userId);
        if (userSettings == null) {
            return;
        }
        try {
            DocumentContext dcSettings = JsonPath.parse(userSettings.getSettings().toString());
            for (String s : jsonPaths) {
                dcSettings = dcSettings.delete("$." + s);
            }
            userSettings.setSettings(new ObjectMapper().readValue(dcSettings.jsonString(), ObjectNode.class));
        } catch (Exception t) {
            handleEvictEvent(new UserSettingsEvictEvent(userSettings.getUserId()));
            throw new RuntimeException(t);
        }
        doSaveUserSettings(tenantId, userSettings);
    }

    private UserSettings doSaveUserSettings(TenantId tenantId, UserSettings userSettings) {
        try {
            validateJsonKeys(userSettings.getSettings());
            UserSettings saved = userSettingsDao.save(tenantId, userSettings);
            publishEvictEvent(new UserSettingsEvictEvent(userSettings.getUserId()));
            return saved;
        } catch (Exception t) {
            handleEvictEvent(new UserSettingsEvictEvent(userSettings.getUserId()));
            throw t;
        }
    }

    @TransactionalEventListener(classes = UserSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(UserSettingsEvictEvent event) {
        List<UserId> keys = new ArrayList<>();
        keys.add(event.getUserId());
        cache.evict(keys);
    }

    private void validateJsonKeys(JsonNode userSettings) {
        Iterator<String> fieldNames = userSettings.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.contains(".") || fieldName.contains(",")) {
                throw new DataValidationException("Json field name should not contain \".\" or \",\" symbols");
            }
        }
    }

    public JsonNode update(JsonNode mainNode, JsonNode updateNode) {
        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldExpression = fieldNames.next();
            String[] fieldPath = fieldExpression.trim().split("\\.");
            var node = (ObjectNode) mainNode;
            for (int i = 0; i < fieldPath.length; i++) {
                var fieldName = fieldPath[i];
                var last = i == (fieldPath.length - 1);
                if (last) {
                    node.set(fieldName, updateNode.get(fieldExpression));
                } else {
                    if (!node.has(fieldName)) {
                        node.set(fieldName, JacksonUtil.newObjectNode());
                    }
                    node = (ObjectNode) node.get(fieldName);
                }
            }
        }
        return mainNode;
    }

}
