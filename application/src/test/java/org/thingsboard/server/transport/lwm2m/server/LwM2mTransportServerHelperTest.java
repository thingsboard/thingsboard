/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

class LwM2mTransportServerHelperTest {

    public static final String KEY_SW_STATE = "sw_state";
    public static final String DOWNLOADING = "DOWNLOADING";

    long now;
    List<TransportProtos.KeyValueProto> kvList;
    ConcurrentMap<String, AtomicLong> keyTsLatestMap;
    LwM2mTransportServerHelper helper;
    LwM2mTransportContext context;


    @BeforeEach
    void setUp() {
        now = System.currentTimeMillis();
        context = mock(LwM2mTransportContext.class);
        helper = spy(new LwM2mTransportServerHelper(context));
        willReturn(now).given(helper).getCurrentTimeMillis();
        kvList = List.of(
                TransportProtos.KeyValueProto.newBuilder().setKey(KEY_SW_STATE).setStringV(DOWNLOADING).build(),
                TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY).setStringV("Transport log example").build()
        );
        keyTsLatestMap = new ConcurrentHashMap<>();
    }

    @Test
    void givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyNoGetTsByKeyCall() {
        assertThat(helper.getTs(null, null)).isEqualTo(now);
        assertThat(helper.getTs(null, keyTsLatestMap)).isEqualTo(now);
        assertThat(helper.getTs(emptyList(), null)).isEqualTo(now);
        assertThat(helper.getTs(emptyList(), keyTsLatestMap)).isEqualTo(now);
        assertThat(helper.getTs(kvList, null)).isEqualTo(now);

        verify(helper, never()).getTsByKey(anyString(), anyMap(), anyLong());
        verify(helper, times(5)).getCurrentTimeMillis();
    }

    @Test
    void givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyGetTsByKeyCallByFirstKey() {
        assertThat(helper.getTs(kvList, keyTsLatestMap)).isEqualTo(now);

        verify(helper, times(1)).getTsByKey(kvList.get(0).getKey(), keyTsLatestMap, now);
        verify(helper, times(1)).getTsByKey(anyString(), anyMap(), anyLong());
    }

    @Test
    void givenKeyAndEmptyLatestTsMap_whenGetTsByKey_thenAddToMapAndReturnNow() {
        assertThat(keyTsLatestMap).as("ts latest map before").isEmpty();

        assertThat(helper.getTsByKey(KEY_SW_STATE, keyTsLatestMap, now)).as("getTsByKey").isEqualTo(now);

        assertThat(keyTsLatestMap).as("ts latest map after").hasSize(1);
        assertThat(keyTsLatestMap.get(KEY_SW_STATE)).as("key present").isNotNull();
        assertThat(keyTsLatestMap.get(KEY_SW_STATE).get()).as("ts in map by key").isEqualTo(now);
    }

    @Test
    void givenKeyAndLatestTsMapWithExistedKey_whenGetTsByKey_thenCallSwapOrIncrementMethod() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong());
        keyTsLatestMap.put("other", new AtomicLong());
        assertThat(keyTsLatestMap).as("ts latest map").hasSize(2);
        willReturn(now).given(helper).compareAndSwapOrIncrementTsAtomically(any(AtomicLong.class), anyLong());

        assertThat(helper.getTsByKey(KEY_SW_STATE, keyTsLatestMap, now)).as("getTsByKey").isEqualTo(now);

        verify(helper, times(1)).compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now);
        verify(helper, times(1)).compareAndSwapOrIncrementTsAtomically(any(AtomicLong.class), anyLong());
    }

    @Test
    void givenMapWithTsValueLessThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNow() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(now - 1));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(now);
    }

    @Test
    void givenMapWithTsValueEqualsNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNowIncremented() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(now));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(now + 1);
    }

    @Test
    void givenMapWithTsValueGreaterThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnGreaterThanNowIncremented() {
        final long nextHourTs = now + TimeUnit.HOURS.toMillis(1);
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(nextHourTs));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(nextHourTs + 1);
    }

}
