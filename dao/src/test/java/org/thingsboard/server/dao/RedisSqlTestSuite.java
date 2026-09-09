// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;

@RunWith(ClasspathSuite.class)
@ClassnameFilters(
        //All the same tests using redis instead of caffeine.
        "org.thingsboard.server.dao.service.*ServiceSqlTest"
)
public class RedisSqlTestSuite extends AbstractRedisContainer {

}
