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
package org.thingsboard.server.dao;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;

@RunWith(ClasspathSuite.class)
@ClassnameFilters({
        "org.thingsboard.server.dao.sql.tenant.*Test",
        "org.thingsboard.server.dao.sql.component.*Test",
        "org.thingsboard.server.dao.sql.customer.*Test",
        "org.thingsboard.server.dao.sql.dashboard.*Test",
        "org.thingsboard.server.dao.sql.query.*Test",
        "org.thingsboard.server.dao.sql.*THIS_MUST_BE_FIXED_Test",
})
public class JpaDaoTestSuite {

}
