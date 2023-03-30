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
package org.thingsboard.server.service.entitiy.user;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.AbstractUserDashboardInfo;
import org.thingsboard.server.common.data.settings.LastVisitedDashboardInfo;
import org.thingsboard.server.common.data.settings.StarredDashboardInfo;
import org.thingsboard.server.common.data.settings.UserDashboardAction;
import org.thingsboard.server.common.data.settings.UserDashboardsInfo;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.user.UserSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbUserSettingsService implements TbUserSettingsService {

    private static final int MAX_DASHBOARD_INFO_LIST_SIZE = 10;
    private static final Predicate<HasTitle> EMPTY_TITLE = i -> StringUtils.isEmpty(i.getTitle());

    private final UserSettingsService settingsService;
    private final DashboardService dashboardService;

    @Override
    public UserSettings saveUserSettings(TenantId tenantId, UserSettings userSettings) {
        return settingsService.saveUserSettings(tenantId, userSettings);
    }

    @Override
    public void updateUserSettings(TenantId tenantId, UserId userId, JsonNode settings) {
        updateUserSettings(tenantId, userId, UserSettings.GENERAL, settings);
    }

    @Override
    public void updateUserSettings(TenantId tenantId, UserId userId, String type, JsonNode settings) {
        settingsService.updateUserSettings(tenantId, userId, type, settings);
    }

    @Override
    public UserSettings findUserSettings(TenantId tenantId, UserId userId) {
        return findUserSettings(tenantId, userId, UserSettings.GENERAL);
    }

    @Override
    public UserSettings findUserSettings(TenantId tenantId, UserId userId, String type) {
        return settingsService.findUserSettings(tenantId, userId, type);
    }

    @Override
    public void deleteUserSettings(TenantId tenantId, UserId userId, List<String> jsonPaths) {
        deleteUserSettings(tenantId, userId, UserSettings.GENERAL, jsonPaths);
    }

    @Override
    public void deleteUserSettings(TenantId tenantId, UserId userId, String type, List<String> jsonPaths) {
        settingsService.deleteUserSettings(tenantId, userId, type, jsonPaths);
    }

    @Override
    public UserDashboardsInfo findUserDashboardsInfo(TenantId tenantId, UserId id) {
        UserSettings us = findUserSettings(tenantId, id, UserSettings.STARRED_DASHBOARDS);
        if (us == null) {
            return UserDashboardsInfo.EMPTY;
        }
        UserDashboardsInfo stored = JacksonUtil.convertValue(us.getSettings(), UserDashboardsInfo.class);
        return getUserDashboardsInfo(tenantId, stored);
    }

    @Override
    public UserDashboardsInfo reportUserDashboardAction(TenantId tenantId, UserId id, DashboardId dashboardId, UserDashboardAction action) {
        UserSettings us = findUserSettings(tenantId, id, UserSettings.STARRED_DASHBOARDS);
        UserDashboardsInfo stored = null;
        if (us != null) {
            stored = JacksonUtil.convertValue(us.getSettings(), UserDashboardsInfo.class);
        }
        if (stored == null) {
            stored = new UserDashboardsInfo();
        }

        switch (action) {
            case STAR:
                addToStarred(stored, dashboardId);
                break;
            case UNSTAR:
                removeFromStarred(stored, dashboardId);
                break;
            case VISIT:
                addToVisited(stored, dashboardId);
                break;
        }

        us = new UserSettings();
        us.setUserId(id);
        us.setType(UserSettings.STARRED_DASHBOARDS);
        us.setSettings(JacksonUtil.valueToTree(stored));
        saveUserSettings(tenantId, us);
        return getUserDashboardsInfo(tenantId, stored);
    }

    private void addToVisited(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        long ts = System.currentTimeMillis();
        var opt = stored.getLast().stream().filter(d -> id.equals(d.getId())).findFirst();
        if (opt.isPresent()) {
            opt.get().setLastVisited(ts);
        } else {
            var newInfo = new LastVisitedDashboardInfo();
            newInfo.setId(id);
            newInfo.setStarred(stored.getStarred().stream().anyMatch(d -> id.equals(d.getId())));
            newInfo.setLastVisited(System.currentTimeMillis());
            stored.getLast().add(newInfo);
        }
        stored.getLast().sort(Comparator.comparing(LastVisitedDashboardInfo::getLastVisited).reversed());
        if (stored.getLast().size() > MAX_DASHBOARD_INFO_LIST_SIZE) {
            stored.setLast(stored.getLast().stream().limit(MAX_DASHBOARD_INFO_LIST_SIZE).collect(Collectors.toList()));
        }
    }

    private void removeFromStarred(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        stored.getStarred().removeIf(d -> id.equals(d.getId()));
        stored.getLast().stream().filter(d -> id.equals(d.getId())).findFirst().ifPresent(d -> d.setStarred(false));
    }

    private void addToStarred(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        long ts = System.currentTimeMillis();
        var opt = stored.getStarred().stream().filter(d -> id.equals(d.getId())).findFirst();
        if (opt.isPresent()) {
            opt.get().setStarredAt(ts);
        } else {
            var newInfo = new StarredDashboardInfo();
            newInfo.setId(id);
            newInfo.setStarredAt(System.currentTimeMillis());
            stored.getStarred().add(newInfo);
        }
        stored.getStarred().sort(Comparator.comparing(StarredDashboardInfo::getStarredAt).reversed());
        if (stored.getStarred().size() > MAX_DASHBOARD_INFO_LIST_SIZE) {
            stored.setStarred(stored.getStarred().stream().limit(MAX_DASHBOARD_INFO_LIST_SIZE).collect(Collectors.toList()));
        }
        Set<UUID> starredMap =
                stored.getStarred().stream().map(AbstractUserDashboardInfo::getId).collect(Collectors.toSet());
        stored.getLast().forEach(d -> d.setStarred(starredMap.contains(d.getId())));
    }

    private void setTitleIfEmpty(TenantId tenantId, AbstractUserDashboardInfo i) {
        if (StringUtils.isEmpty(i.getTitle())) {
            var dashboardInfo = dashboardService.findDashboardInfoById(tenantId, new DashboardId(i.getId()));
            i.setTitle(dashboardInfo != null ? dashboardInfo.getTitle() : null);
        }
    }

    private UserDashboardsInfo getUserDashboardsInfo(TenantId tenantId, UserDashboardsInfo stored) {
        if (stored == null) {
            return UserDashboardsInfo.EMPTY;
        }

        if (!stored.getLast().isEmpty()) {
            stored.getLast().forEach(i -> setTitleIfEmpty(tenantId, i));
            stored.getLast().removeIf(EMPTY_TITLE);
        }
        if (!stored.getStarred().isEmpty()) {
            Map<UUID, LastVisitedDashboardInfo> lastMap = stored.getLast().stream().collect(Collectors.toMap(LastVisitedDashboardInfo::getId, Function.identity()));
            stored.getStarred().forEach(i -> {
                var last = lastMap.get(i.getId());
                i.setTitle(last != null ? last.getTitle() : null);
            });
            stored.getStarred().forEach(i -> setTitleIfEmpty(tenantId, i));
            stored.getStarred().removeIf(EMPTY_TITLE);
        }
        return stored;
    }

}
