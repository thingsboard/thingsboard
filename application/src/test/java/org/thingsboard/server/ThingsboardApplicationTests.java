/**
 * Copyright © 2016 The Thingsboard Authors
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ThingsboardServerApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class ThingsboardApplicationTests {

    @Test
    public void contextLoads() {
        String test = "[   \n" +
                "    {\n" +
                "        \"key\": \"name\",\n" +
                "\t\"type\": \"text\"        \n" +
                "    },\n" +
                "    {\n" +
                "\t\"key\": \"name2\",\n" +
                "\t\"type\": \"color\"\n" +
                "    },\n" +
                "    {\n" +
                "\t\"key\": \"name3\",\n" +
                "\t\"type\": \"javascript\"\n" +
                "    }    \n" +
                "]";
    }

}
