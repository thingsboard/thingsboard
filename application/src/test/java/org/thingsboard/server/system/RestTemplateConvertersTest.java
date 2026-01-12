/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Slf4j
public class RestTemplateConvertersTest {

    @Test
    public void testMappingJackson2HttpMessageConverterIsUsedInsteadOfMappingJackson2XmlHttpMessageConverter() {
        ClassLoader classLoader = RestTemplate.class.getClassLoader();
        boolean jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
        assertThat(jackson2XmlPresent).isTrue();

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("/test"))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    byte[] body = mockRequest.getBodyAsBytes();
                    String requestBody = new String(body, StandardCharsets.UTF_8);
                    assertThat(requestBody).contains("{\"name\":\"test\",\"value\":1}");
                })
                .andRespond(withSuccess("{\"name\":\"test\",\"value\":1}", MediaType.APPLICATION_JSON));

        TestObject requestObject = new TestObject("test", 1);
        TestObject actualObject = restTemplate.postForObject("/test", requestObject, TestObject.class);
        assertThat(actualObject).isEqualTo(requestObject);
        mockServer.verify();
    }

    record TestObject(String name, int value) {}

}
