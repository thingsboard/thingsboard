/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.ListeningExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@Component
public class JsExecutorService implements ListeningExecutor{

    @Value("${actors.rule.js_thread_pool_size}")
    private int jsExecutorThreadPoolSize;

    private ListeningExecutorService service;

    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(jsExecutorThreadPoolSize));
    }

    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        return service.submit(task);
    }

}
