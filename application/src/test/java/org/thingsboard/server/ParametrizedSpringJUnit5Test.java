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
package org.thingsboard.server;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

@DaoSqlTest
@Slf4j
public class ParametrizedSpringJUnit5Test extends AbstractControllerTest {

    @Autowired
    ApplicationContext applicationContext;
    @LocalServerPort
    private int port;

    @BeforeAll
    static void beforeAll() {
        log.warn("beforeAll");
    }

    @BeforeEach
    void setUp() {
        log.warn("BeforeEach: port [{}], app context [{}]", port, applicationContext);
    }

    @Test
    void testOnce() {
        log.warn("test once");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 7, -1, 100, Integer.MAX_VALUE})
    void testParametrized(int number) {
        log.warn("test parameter [{}]", number);

    }
}
