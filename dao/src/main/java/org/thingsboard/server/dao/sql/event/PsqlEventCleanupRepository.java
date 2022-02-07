/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.PsqlDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@Slf4j
@PsqlDao
@Repository
public class PsqlEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements EventCleanupRepository {

    @Override
    public void cleanupEvents(long otherEventsTtl, long debugEventsTtl) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_events_by_ttl(?,?,?)")) {
            stmt.setLong(1, otherEventsTtl);
            stmt.setLong(2, debugEventsTtl);
            stmt.setLong(3, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()){
                resultSet.next();
                log.info("Total events removed by TTL: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during events TTL task execution ", e);
        }
    }

}
