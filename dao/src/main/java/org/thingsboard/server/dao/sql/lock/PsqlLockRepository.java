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
package org.thingsboard.server.dao.sql.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@SqlDao
@PsqlDao
@Repository
public class PsqlLockRepository implements LockRepository {
    private static final String TRANSACTION_LOCK_QUERY = "SELECT pg_advisory_xact_lock(?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void transactionLock(Integer key) {
        jdbcTemplate.query(TRANSACTION_LOCK_QUERY,
                preparedStatement -> preparedStatement.setInt(1, key),
                resultSet -> {}
        );
    }
}
