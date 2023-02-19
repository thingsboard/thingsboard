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
package org.thingsboard.common.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class LinkedHashMapRemoveEldestTest {

    public static final long MAX_ENTRIES = 10L;
    long removeCount = 0;

    void removalConsumer(Long id, String name) {
        removeCount++;
        assertThat(id, is(Matchers.lessThan(MAX_ENTRIES)));
        assertThat(name, is(id.toString()));
    }

    @Test
    public void givenMap_whenOverSized_thenVerifyRemovedEldest() {
        //given
        LinkedHashMapRemoveEldest<Long, String> map =
                new LinkedHashMapRemoveEldest<>(MAX_ENTRIES, this::removalConsumer);

        assertThat(map.getMaxEntries(), is(MAX_ENTRIES));
        assertThat(map.getRemovalConsumer(), notNullValue());
        assertThat(map, instanceOf(LinkedHashMap.class));
        assertThat(map, instanceOf(LinkedHashMapRemoveEldest.class));
        assertThat(map.size(), is(0));

        //when
        for (long i = 0; i < MAX_ENTRIES * 2; i++) {
            map.put(i, String.valueOf(i));
        }

        //then
        assertThat((long) map.size(), is(MAX_ENTRIES));
        assertThat(removeCount, is(MAX_ENTRIES));
        for (long i = MAX_ENTRIES; i < MAX_ENTRIES * 2; i++) {
            assertThat(map.get(i), is(String.valueOf(i)));
        }
    }

}