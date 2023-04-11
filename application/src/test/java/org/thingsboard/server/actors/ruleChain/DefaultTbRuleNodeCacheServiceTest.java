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
package org.thingsboard.server.actors.ruleChain;

import com.google.protobuf.ByteString;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.rule.engine.api.RuleNodeCacheService;
import org.thingsboard.server.cache.rule.RuleNodeCache;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTbRuleNodeCacheServiceTest {

    private static final TbMsgMetaData EMPTY_METADATA = new TbMsgMetaData();
    private static final String EMPTY_DATA = "{}";
    
    private static final RandomDataGenerator randomPartitionGenerator = new RandomDataGenerator();

    private static RuleNodeId ruleNodeId;
    private static RuleNodeCache cache;
    private static RuleNodeCacheService ruleNodeCacheService;

    @BeforeAll
    public static void beforeTest() {
        ruleNodeId = new RuleNodeId(UUID.randomUUID());
        cache = mock(RuleNodeCache.class);
        ruleNodeCacheService = spy(new DefaultTbRuleNodeCacheService(ruleNodeId, cache));
    }

    @Test
    void test_givenString_whenAdd_thenVerifyCacheCallMethodArguments() {
        String key = RandomStringUtils.randomAlphabetic(5);
        String value = RandomStringUtils.randomAlphabetic(5);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        byte[] cacheValue = value.getBytes();
        ruleNodeCacheService.add(key, value);
        verify(cache, times(1)).add(eq(cacheKey), eq(cacheValue));
    }

    @Test
    void test_givenEntityId_whenAdd_thenVerifyCacheCallMethodArguments() {
        String key = RandomStringUtils.randomAlphabetic(10);
        EntityId entityId = new DeviceId(UUID.randomUUID());
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        byte[] cacheValue = SerializationUtils.serialize(entityId);
        ruleNodeCacheService.add(key, entityId);
        verify(cache, times(1)).add(eq(cacheKey), eq(cacheValue));
    }

    @Test
    void test_givenTbMsg_whenAdd_thenVerifyCacheCallMethodArguments() {
        String msgType = SessionMsgType.POST_TELEMETRY_REQUEST.name();
        EntityId originator = new DeviceId(UUID.randomUUID());
        TbMsg msg = TbMsg.newMsg(
                msgType,
                originator,
                EMPTY_METADATA,
                EMPTY_DATA
        );
        int partition = randomPartitionGenerator.nextInt(0, 10);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, partition, originator);
        byte[] cacheValue = TbMsg.toByteArray(msg);
        ruleNodeCacheService.add(originator, partition, msg);
        verify(cache, times(1)).add(eq(cacheKey), eq(cacheValue));
    }

    @Test
    void test_givenStrings_whenRemoveStringList_thenVerifyCacheCallMethodArguments() {
        String key = RandomStringUtils.randomAlphabetic(5);
        String value = RandomStringUtils.randomAlphabetic(5);
        List<String> stringList = Collections.singletonList(value);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        byte[][] cacheValues = stringListToBytes(stringList);
        ruleNodeCacheService.removeStringList(key, stringList);
        verify(cache, times(1)).remove(eq(cacheKey), eq(cacheValues));
    }

    @Test
    void test_givenEntityIds_whenRemoveEntityIdList_thenVerifyCacheCallMethodArguments() {
        String key = RandomStringUtils.randomAlphabetic(5);
        EntityId entityId = new DeviceId(UUID.randomUUID());
        List<EntityId> entityIdList = Collections.singletonList(entityId);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        byte[][] cacheValues = entityIdListToBytes(entityIdList);
        ruleNodeCacheService.removeEntityIdList(key, entityIdList);
        verify(cache, times(1)).remove(eq(cacheKey), eq(cacheValues));
    }

    @Test
    void test_givenTbMsgs_whenRemoveTbMsgList_thenVerifyCacheCallMethodArguments() {
        String msgType = SessionMsgType.POST_TELEMETRY_REQUEST.name();
        EntityId originator = new DeviceId(UUID.randomUUID());
        TbMsg msg = TbMsg.newMsg(
                msgType,
                originator,
                EMPTY_METADATA,
                EMPTY_DATA
        );
        List<TbMsg> tbMsgList = Collections.singletonList(msg);
        int partition = randomPartitionGenerator.nextInt(0, 10);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, partition, originator);
        byte[][] cacheValues = tbMsgListToBytes(tbMsgList);
        ruleNodeCacheService.removeTbMsgList(originator, partition, tbMsgList);
        verify(cache, times(1)).remove(eq(cacheKey), eq(cacheValues));
    }

    @Test
    void test_whenGetStrings_thenVerifyCacheCallMethodArgumentsAndResult() {
        String key = RandomStringUtils.randomAlphabetic(5);
        String value = RandomStringUtils.randomAlphabetic(5);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        Set<byte[]> cacheValuesInBytes = Collections.singleton(value.getBytes());
        Set<String> expectedSet = Collections.singleton(value);

        when(cache.get(eq(cacheKey))).thenReturn(cacheValuesInBytes);
        Set<String> actualSet = ruleNodeCacheService.getStrings(key);

        verify(cache, times(1)).get(eq(cacheKey));
        Assertions.assertEquals(expectedSet, actualSet);
    }

    @Test
    void test_whenGetEntityIds_thenVerifyCacheCallMethodArgumentsAndResult() {
        String key = RandomStringUtils.randomAlphabetic(5);
        EntityId entityId = new DeviceId(UUID.randomUUID());
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        Set<byte[]> cacheValuesInBytes = Collections.singleton(SerializationUtils.serialize(entityId));
        Set<EntityId> expectedSet = Collections.singleton(entityId);

        when(cache.get(eq(cacheKey))).thenReturn(cacheValuesInBytes);
        Set<EntityId> actualSet = ruleNodeCacheService.getEntityIds(key);

        verify(cache, times(1)).get(eq(cacheKey));
        Assertions.assertEquals(expectedSet, actualSet);
    }

    @Test
    void test_whenGetTbMsgs_thenVerifyCacheCallMethodArgumentsAndResult() {
        String msgType = SessionMsgType.POST_TELEMETRY_REQUEST.name();
        EntityId originator = new DeviceId(UUID.randomUUID());
        TbMsg msg = TbMsg.newMsg(
                msgType,
                originator,
                EMPTY_METADATA,
                EMPTY_DATA
        );
        ByteString expectedTbMsgByteString = TbMsg.toByteString(msg);
        Set<byte[]> cacheValuesInBytes = Collections.singleton(TbMsg.toByteArray(msg));
        int partition = randomPartitionGenerator.nextInt(0, 10);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, partition, originator);

        when(cache.get(eq(cacheKey))).thenReturn(cacheValuesInBytes);
        Set<TbMsg> actualTbMsgSet = ruleNodeCacheService.getTbMsgs(originator, partition);

        verify(cache, times(1)).get(eq(cacheKey));
        Optional<TbMsg> first = actualTbMsgSet.stream().findFirst();
        Assertions.assertTrue(first.isPresent());
        ByteString actualTbMsgByteString = TbMsg.toByteString(first.get());
        Assertions.assertEquals(expectedTbMsgByteString, actualTbMsgByteString);
    }

    @Test
    void test_givenStringKey_whenEvict_thenVerifyCacheCallMethodArguments() {
        String key = RandomStringUtils.randomAlphabetic(5);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, key);
        ruleNodeCacheService.evict(key);
        verify(cache, times(1)).evict(eq(cacheKey));
    }

    @Test
    void test_givenEntityIdKeyAndPartition_whenEvictTbMsgs_thenVerifyCacheCallMethodArguments() {
        EntityId key = new DeviceId(UUID.randomUUID());
        int partition = randomPartitionGenerator.nextInt(0, 10);
        byte[] cacheKey = toRuleNodeCacheKey(ruleNodeId, partition, key);
        ruleNodeCacheService.evictTbMsgs(key, partition);
        verify(cache, times(1)).evict(eq(cacheKey));
    }

    private byte[][] stringListToBytes(List<String> values) {
        return values.stream()
                .map(String::getBytes)
                .toArray(byte[][]::new);
    }

    private byte[][] entityIdListToBytes(List<EntityId> values) {
        return values.stream()
                .map(SerializationUtils::serialize)
                .toArray(byte[][]::new);
    }

    private byte[][] tbMsgListToBytes(List<TbMsg> values) {
        return values.stream()
                .map(TbMsg::toByteArray)
                .toArray(byte[][]::new);
    }
    private byte[] toRuleNodeCacheKey(RuleNodeId ruleNodeId, String key) {
        return String.format("%s::%s", ruleNodeId.getId().toString(), key).getBytes();
    }

    private byte[] toRuleNodeCacheKey(RuleNodeId ruleNodeId, Integer partition, EntityId key) {
        return String.format("%s::%s::%s", ruleNodeId.getId().toString(), partition, key.getId().toString()).getBytes();
    }
}
