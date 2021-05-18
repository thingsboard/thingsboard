/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.common.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LinkedHashMapRemoveEldestTest {

    public static final long MAX_ENTRIES = 10L;
    long removeCount = 0;

    void removeConsumer(Long id, String name) {
        removeCount++;
        assertThat(id, is(Matchers.lessThan(MAX_ENTRIES)));
        assertThat(name, is(id.toString()));
    }

    @Test
    public void givenMap_whenOverSized_thenVerifyRemovedEldest() {
        //given
        LinkedHashMapRemoveEldest<Long, String> map =
                new LinkedHashMapRemoveEldest<>(MAX_ENTRIES, this::removeConsumer);

        assertThat(map.getMaxEntries(), is(MAX_ENTRIES));
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