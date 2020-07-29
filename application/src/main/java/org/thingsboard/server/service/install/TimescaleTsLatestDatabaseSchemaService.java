/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.PsqlTsLatestAnyDao;
import org.thingsboard.server.dao.util.TimescaleDBTsLatestDao;

@Service
@TimescaleDBTsLatestDao
@Profile("install")
@Slf4j
public class TimescaleTsLatestDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements TsLatestDatabaseSchemaService {

    public TimescaleTsLatestDatabaseSchemaService() {
        super("schema-ts-latest-timescale.sql", null);
    }
}