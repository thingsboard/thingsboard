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
package org.thingsboard.server.rules;

import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.thingsboard.server.dao.CustomCassandraCQLUnit;
import org.thingsboard.server.dao.CustomSqlUnit;
import org.thingsboard.server.queue.memory.InMemoryStorage;

import java.util.Arrays;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
        "org.thingsboard.server.rules.flow.nosql.*Test",
        "org.thingsboard.server.rules.lifecycle.nosql.*Test"
})
public class RuleEngineNoSqlTestSuite {

    @ClassRule
    public static CustomCassandraCQLUnit cassandraUnit =
            new CustomCassandraCQLUnit(
                    Arrays.asList(
                            new ClassPathCQLDataSet("cassandra/schema-ts.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/schema-entities.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/system-data.cql", false, false)),
                    "cassandra-test.yaml", 30000l);

    @BeforeClass
    public static void cleanupInMemStorage(){
        InMemoryStorage.getInstance().cleanup();
    }

}
