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
package org.thingsboard.server.dao.sql.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.UserSettings;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.EntityAlarmEntity;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.model.sql.UserCredentialsEntity;
import org.thingsboard.server.dao.model.sql.UserSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.user.UserCredentialsDao;
import org.thingsboard.server.dao.user.UserSettingsDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaUserSettingsDao extends JpaAbstractDaoListeningExecutorService implements UserSettingsDao {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Override
    public UserSettings saveSettings(TenantId tenantId, UserSettings userSettings) {
        return DaoUtil.getData(userSettingsRepository.save(new UserSettingsEntity(userSettings)));
    }

    @Override
    public UserSettings findByUserId(TenantId tenantId, UserId userId) {
        return DaoUtil.getData(userSettingsRepository.findById(userId.getId()));
    }

    @Override
    public void removeByUserId(TenantId tenantId, UserId userId) {
        userSettingsRepository.deleteById(userId.getId());
    }

}
