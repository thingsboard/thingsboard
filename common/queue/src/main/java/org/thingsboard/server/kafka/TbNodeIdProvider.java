/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by ashvayka on 12.10.18.
 */
@Slf4j
@Component
public class TbNodeIdProvider {

    @Getter
    @Value("${cluster.node_id:#{null}}")
    private String nodeId;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(nodeId)) {
            try {
                nodeId = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                nodeId = org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10);
            }
        }
        log.info("Current NodeId: {}", nodeId);
    }

}
