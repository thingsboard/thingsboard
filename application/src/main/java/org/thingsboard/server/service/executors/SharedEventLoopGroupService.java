/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.executors;

import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class SharedEventLoopGroupService {

    @Getter
    private EventLoopGroup sharedEventLoopGroup;

    @PostConstruct
    public void init() {
        this.sharedEventLoopGroup = new NioEventLoopGroup();
    }

    @PreDestroy
    public void destroy() {
        if (this.sharedEventLoopGroup != null) {
            this.sharedEventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

}
