// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class JsonMapperWiringTest extends AbstractControllerTest {

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void mvcUsesJackson2() {
        List<HttpMessageConverter<?>> converters = handlerAdapter.getMessageConverters();
        assertThat(converters).noneMatch(c -> c.getClass().getName().startsWith("org.springframework.http.converter.json.JacksonJson"));
        MappingJackson2HttpMessageConverter json = converters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .findFirst().orElseThrow();
        assertThat(json.getObjectMapper()).isSameAs(objectMapper);
        assertThat(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
    }

}
