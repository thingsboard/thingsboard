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
package org.thingsboard.server.system;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;


@Slf4j
public class RestTemplateConvertersTest {

    @Test
    public void testJacksonXmlConverter() {
        ClassLoader classLoader = RestTemplate.class.getClassLoader();
        boolean jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
        Assertions.assertFalse(jackson2XmlPresent, "XmlMapper must not be present in classpath, please, exclude \"jackson-dataformat-xml\" dependency!");
        //If this xml mapper will be present in classpath then we will get "Unsupported Media Type" in RestTemplate
    }

}
