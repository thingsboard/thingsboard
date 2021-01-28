/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.util;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.utils.EventDeduplicationExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class EventDeduplicationExecutorTest {

    @Test
    public void testSimpleFlowSameThread() throws InterruptedException {
        simpleFlow(MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void testPeriodicFlowSameThread() throws InterruptedException {
        periodicFlow(MoreExecutors.newDirectExecutorService());
    }


    @Test
    public void testSimpleFlowSingleThread() throws InterruptedException {
        simpleFlow(Executors.newFixedThreadPool(1));
    }

    @Test
    public void testPeriodicFlowSingleThread() throws InterruptedException {
        periodicFlow(Executors.newFixedThreadPool(1));
    }

    @Test
    public void testSimpleFlowMultiThread() throws InterruptedException {
        simpleFlow(Executors.newFixedThreadPool(3));
    }

    @Test
    public void testPeriodicFlowMultiThread() throws InterruptedException {
        periodicFlow(Executors.newFixedThreadPool(3));
    }

    private void simpleFlow(ExecutorService executorService) throws InterruptedException {
        try {
            Consumer<String> function = Mockito.spy(StringConsumer.class);
            EventDeduplicationExecutor<String> executor = new EventDeduplicationExecutor<>(EventDeduplicationExecutorTest.class.getSimpleName(), executorService, function);

            String params1 = "params1";
            String params2 = "params2";
            String params3 = "params3";

            executor.submit(params1);
            executor.submit(params2);
            executor.submit(params3);
            Thread.sleep(500);
            Mockito.verify(function).accept(params1);
            Mockito.verify(function).accept(params3);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void periodicFlow(ExecutorService executorService) throws InterruptedException {
        try {
            Consumer<String> function = Mockito.spy(StringConsumer.class);
            EventDeduplicationExecutor<String> executor = new EventDeduplicationExecutor<>(EventDeduplicationExecutorTest.class.getSimpleName(), executorService, function);

            String params1 = "params1";
            String params2 = "params2";
            String params3 = "params3";

            executor.submit(params1);
            Thread.sleep(500);
            executor.submit(params2);
            Thread.sleep(500);
            executor.submit(params3);
            Thread.sleep(500);
            Mockito.verify(function).accept(params1);
            Mockito.verify(function).accept(params2);
            Mockito.verify(function).accept(params3);
        } finally {
            executorService.shutdownNow();
        }
    }

    public static class StringConsumer implements Consumer<String> {
        @Override
        public void accept(String s) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
