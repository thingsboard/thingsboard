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
package org.thingsboard.server.dao;

import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;

@ContextConfiguration(initializers = RedisSqlTestSuite.class)
@RunWith(ClasspathSuite.class)
@ClassnameFilters(
        //All the same tests using redis instead of caffeine.
        "org.thingsboard.server.dao.service.*ServiceSqlTest"
)
public class RedisSqlTestSuite implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @ClassRule
    public static GenericContainer redis = new GenericContainer("redis:4.0").withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "cache.type=redis");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.connection.type=standalone");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.standalone.host=localhost");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.standalone.port=" + redis.getMappedPort(6379));
    }

}
