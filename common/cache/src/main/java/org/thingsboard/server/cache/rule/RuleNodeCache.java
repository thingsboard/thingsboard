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
package org.thingsboard.server.cache.rule;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Set;

public interface RuleNodeCache {

    void add(RuleNodeId ruleNodeId, String key, String value);

    void add(RuleNodeId ruleNodeId, String key, EntityId value);

    void add(RuleNodeId ruleNodeId, Integer partition, EntityId key, TbMsg value);

    void removeStringList(RuleNodeId ruleNodeId, String key, List<String> values);

    void removeEntityIdList(RuleNodeId ruleNodeId, String key, List<EntityId> values);

    void removeTbMsgList(RuleNodeId ruleNodeId, Integer partition, EntityId key, List<TbMsg> values);

    Set<String> getStringSetByKey(RuleNodeId ruleNodeId, String key);

    Set<EntityId> getEntityIdSetByKey(RuleNodeId ruleNodeId, String key);

    Set<TbMsg> getTbMsgSetByKey(RuleNodeId ruleNodeId, Integer partition, EntityId key);

    void evict(RuleNodeId ruleNodeId, String key);

    void evict(RuleNodeId ruleNodeId, Integer partition, EntityId key);

}
