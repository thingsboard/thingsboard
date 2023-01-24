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
package org.thingsboard.server.service.rule;

import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Set;

public interface RuleNodeCacheService {

    void add(RuleNodeId ruleNodeId, String key, byte[]... values);

    void add(RuleNodeId ruleNodeId, String key, TbMsg value);

    void add(RuleNodeId ruleNodeId, String key, List<TbMsg> values);

    Set<byte[]> get(RuleNodeId ruleNodeId, String key);

    Set<TbMsg> get(RuleNodeId ruleNodeId, String key, String queueName);

    void remove(RuleNodeId ruleNodeId, String key, byte[]... values);

    void remove(RuleNodeId ruleNodeId, String key, TbMsg value);

    void remove(RuleNodeId ruleNodeId, String key, List<TbMsg> values);

    void evict(RuleNodeId ruleNodeId, String key);
}
