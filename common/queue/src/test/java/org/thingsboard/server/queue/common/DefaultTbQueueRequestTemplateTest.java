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
package org.thingsboard.server.queue.common;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTbQueueRequestTemplateTest {

    @Mock
    TbQueueAdmin queueAdmin;
    @Mock
    ExecutorService executor;
    @Mock
    TbQueueProducer<TbQueueMsg> requestTemplate;
    @Mock
    TbQueueConsumer<TbQueueMsg> responseTemplate;

    long maxRequestTimeout = 20;
    long maxPendingRequests = 10000;
    long pollInterval = 25;
    String topic = "js-responses-tb-node-0";

    DefaultTbQueueRequestTemplate inst;

    @Before
    public void setUp() throws Exception {
        willReturn(topic).given(responseTemplate).getTopic();
        inst = new DefaultTbQueueRequestTemplate(
                queueAdmin, requestTemplate, responseTemplate,
                maxRequestTimeout, maxPendingRequests, pollInterval, executor);

    }

    @Test
    public void givenExternalExecutor_whenStop_thenDoNotShutdownExecutor() {
        inst = Mockito.spy(inst);
        willDoNothing().given(inst).fetchAndProcessResponses();
        Assert.assertFalse(inst.stopped);

        inst.init();
        Assert.assertNotEquals(inst.tickTs, 0);
        Assert.assertFalse(inst.internalExecutor);
        verify(queueAdmin, times(1)).createTopicIfNotExists(topic);
        verify(requestTemplate, times(1)).init();
        verify(responseTemplate, times(1)).subscribe();
        verify(executor, times(1)).submit(any(Runnable.class));

        inst.stop();
        Assert.assertTrue(inst.stopped);
        verify(responseTemplate, times(1)).unsubscribe();
        verify(requestTemplate, times(1)).stop();
        verify(executor, never()).shutdownNow();
    }

    @Test
    public void fetchAndProcessResponses() {
        inst.init();

        inst.stop();
    }
}