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
package org.thingsboard.server.common.msg.queue;

import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicPartitionInfoTest {

    @Test
    public void givenTopicPartitionInfo_whenEquals_thenTrue() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()));

    }

    @Test
    public void givenTopicPartitionInfo_whenEquals_thenFalse() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

    }
}