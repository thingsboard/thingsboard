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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.rule.RuleNodeCache;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultRuleNodeCacheService implements RuleNodeCacheService {

    private final RuleNodeCache ruleNodeCache;

    @Override
    public void add(RuleNodeId ruleNodeId, String key, byte[]... values) {
        ruleNodeCache.add(toRuleNodeCacheKey(ruleNodeId, key), values);
    }

    @Override
    public void add(RuleNodeId ruleNodeId, String key, TbMsg value) {
        ruleNodeCache.add(toRuleNodeCacheKey(ruleNodeId, key), TbMsg.toByteArray(value));
    }

    @Override
    public void add(RuleNodeId ruleNodeId, String key, List<TbMsg> values) {
        ruleNodeCache.add(toRuleNodeCacheKey(ruleNodeId, key), tbMsgListToBytes(values));
    }

    @Override
    public Set<byte[]> get(RuleNodeId ruleNodeId, String key) {
        return ruleNodeCache.get(toRuleNodeCacheKey(ruleNodeId, key));
    }

    @Override
    public Set<TbMsg> get(RuleNodeId ruleNodeId, String key, String queueName) {
        return ruleNodeCache.get(toRuleNodeCacheKey(ruleNodeId, key))
                .stream()
                .map(bytes -> TbMsg.fromBytes(queueName, bytes, TbMsgCallback.EMPTY))
                .collect(Collectors.toSet());
    }

    @Override
    public void remove(RuleNodeId ruleNodeId, String key, byte[]... values) {
        ruleNodeCache.remove(toRuleNodeCacheKey(ruleNodeId, key), values);
    }

    @Override
    public void remove(RuleNodeId ruleNodeId, String key, TbMsg value) {
        ruleNodeCache.remove(toRuleNodeCacheKey(ruleNodeId, key), TbMsg.toByteArray(value));
    }

    @Override
    public void remove(RuleNodeId ruleNodeId, String key, List<TbMsg> values) {
        ruleNodeCache.remove(toRuleNodeCacheKey(ruleNodeId, key), tbMsgListToBytes(values));
    }

    @Override
    public void evict(RuleNodeId ruleNodeId, String key) {
        ruleNodeCache.evict(toRuleNodeCacheKey(ruleNodeId, key));
    }

    private String toRuleNodeCacheKey(RuleNodeId ruleNodeId, String key) {
        return String.format("%s::%s", ruleNodeId.toString(), key);
    }

    private byte[][] tbMsgListToBytes(List<TbMsg> values) {
        return values.stream()
                .map(TbMsg::toByteArray)
                .toArray(byte[][]::new);
    }

}
