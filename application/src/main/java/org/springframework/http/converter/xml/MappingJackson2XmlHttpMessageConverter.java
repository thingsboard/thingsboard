// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.springframework.http.converter.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * RestTemplate firstly uses MappingJackson2XmlHttpMessageConverter converter instead of MappingJackson2HttpMessageConverter.
 * It produces error UnsupportedMediaType, so this converter had to be shadowed for read and write operations to use the correct converter
 */
public class MappingJackson2XmlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {
    private static final List<MediaType> problemDetailMediaTypes;

    public MappingJackson2XmlHttpMessageConverter() {
        this(Jackson2ObjectMapperBuilder.xml().build());
    }

    public MappingJackson2XmlHttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper, new MediaType[]{new MediaType("application", "xml", StandardCharsets.UTF_8), new MediaType("text", "xml", StandardCharsets.UTF_8), new MediaType("application", "*+xml", StandardCharsets.UTF_8)});
        Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
        super.setObjectMapper(objectMapper);
    }

    protected List<MediaType> getMediaTypesForProblemDetail() {
        return problemDetailMediaTypes;
    }

    static {
        problemDetailMediaTypes = Collections.singletonList(MediaType.APPLICATION_PROBLEM_XML);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }
}
