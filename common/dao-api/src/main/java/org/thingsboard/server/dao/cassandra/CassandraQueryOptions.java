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
package org.thingsboard.server.dao.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.QueryOptions;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.util.NoSqlAnyDao;

import javax.annotation.PostConstruct;

@Component
@Configuration
@Data
@NoSqlAnyDao
public class CassandraQueryOptions {

    @Value("${cassandra.query.default_fetch_size}")
    private Integer defaultFetchSize;
    @Value("${cassandra.query.read_consistency_level}")
    private String readConsistencyLevel;
    @Value("${cassandra.query.write_consistency_level}")
    private String writeConsistencyLevel;

    private QueryOptions opts;

    private ConsistencyLevel defaultReadConsistencyLevel;
    private ConsistencyLevel defaultWriteConsistencyLevel;

    @PostConstruct
    public void initOpts(){
        opts = new QueryOptions();
        opts.setFetchSize(defaultFetchSize);
    }

    protected ConsistencyLevel getDefaultReadConsistencyLevel() {
        if (defaultReadConsistencyLevel == null) {
            if (readConsistencyLevel != null) {
                defaultReadConsistencyLevel = ConsistencyLevel.valueOf(readConsistencyLevel.toUpperCase());
            } else {
                defaultReadConsistencyLevel = ConsistencyLevel.ONE;
            }
        }
        return defaultReadConsistencyLevel;
    }

    protected ConsistencyLevel getDefaultWriteConsistencyLevel() {
        if (defaultWriteConsistencyLevel == null) {
            if (writeConsistencyLevel != null) {
                defaultWriteConsistencyLevel = ConsistencyLevel.valueOf(writeConsistencyLevel.toUpperCase());
            } else {
                defaultWriteConsistencyLevel = ConsistencyLevel.ONE;
            }
        }
        return defaultWriteConsistencyLevel;
    }
}
