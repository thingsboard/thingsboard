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
package org.thingsboard.server.dao.device;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.DeviceCredentialsEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraDeviceCredentialsDao extends CassandraAbstractModelDao<DeviceCredentialsEntity, DeviceCredentials> implements DeviceCredentialsDao {

    @Override
    protected Class<DeviceCredentialsEntity> getColumnFamilyClass() {
        return DeviceCredentialsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.DEVICE_CREDENTIALS_COLUMN_FAMILY_NAME;
    }

    @Override
    public DeviceCredentials findByDeviceId(UUID deviceId) {
        log.debug("Try to find device credentials by deviceId [{}] ", deviceId);
        Where query = select().from(ModelConstants.DEVICE_CREDENTIALS_BY_DEVICE_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY, deviceId));
        log.trace("Execute query {}", query);
        DeviceCredentialsEntity deviceCredentialsEntity = findOneByStatement(query);
        log.trace("Found device credentials [{}] by deviceId [{}]", deviceCredentialsEntity, deviceId);
        return DaoUtil.getData(deviceCredentialsEntity);
    }
    
    @Override
    public DeviceCredentials findByCredentialsId(String credentialsId) {
        log.debug("Try to find device credentials by credentialsId [{}] ", credentialsId);
        Where query = select().from(ModelConstants.DEVICE_CREDENTIALS_BY_CREDENTIALS_ID_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY, credentialsId));
        log.trace("Execute query {}", query);
        DeviceCredentialsEntity deviceCredentialsEntity = findOneByStatement(query);
        log.trace("Found device credentials [{}] by credentialsId [{}]", deviceCredentialsEntity, credentialsId);
        return DaoUtil.getData(deviceCredentialsEntity);
    }
}
