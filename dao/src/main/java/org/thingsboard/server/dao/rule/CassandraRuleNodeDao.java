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
package org.thingsboard.server.dao.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.model.nosql.RuleNodeEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_COLUMN_FAMILY_NAME;

@Component
@Slf4j
@NoSqlDao
public class CassandraRuleNodeDao extends CassandraAbstractSearchTextDao<RuleNodeEntity, RuleNode> implements RuleNodeDao {

    @Override
    protected Class<RuleNodeEntity> getColumnFamilyClass() {
        return RuleNodeEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return RULE_NODE_COLUMN_FAMILY_NAME;
    }

}
