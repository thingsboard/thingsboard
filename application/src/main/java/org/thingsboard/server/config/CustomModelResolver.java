/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.media.Discriminator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Configuration
public class CustomModelResolver {
    @Bean
    public ModelResolver myCustomModelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper) {
            @Override
            protected Discriminator resolveDiscriminator(JavaType type, ModelConverterContext context) {
                Discriminator discriminator = super.resolveDiscriminator(type, context);
                if(type.getRawClass().equals(AiModelConfig.class)) {
                    System.out.println(discriminator);
                }
                Schema schema = type.getRawClass().getDeclaredAnnotation(Schema.class);
                if(schema != null && schema.discriminatorMapping() != null && schema.discriminatorMapping().length > 0) {
                    return discriminator;
                }
                if (discriminator != null && discriminator.getPropertyName() != null) {
                    addResolvedSubTypeMappings(discriminator, type, context);
                    addAnnotatedSubTypeMappings(discriminator, type, context);
                }

                return discriminator;
            }

            private void addResolvedSubTypeMappings(Discriminator discriminator, JavaType type, ModelConverterContext context) {
                MapperConfig<?> config = _mapper.getSerializationConfig();
                _mapper.getSubtypeResolver()
                        .collectAndResolveSubtypesByClass(config, AnnotatedClassResolver.resolveWithoutSuperTypes(config, type, config))
                        .stream()
                        .filter(namedType -> !namedType.getType().equals(type.getRawClass()))
                        .forEach(namedType -> addMapping(discriminator, namedType.getName(), namedType.getType(), context));
            }

            private void addAnnotatedSubTypeMappings(Discriminator discriminator, JavaType type, ModelConverterContext context) {
                JsonSubTypes jsonSubTypes = type.getRawClass().getDeclaredAnnotation(JsonSubTypes.class);
                if (jsonSubTypes != null) {
                    Arrays.stream(jsonSubTypes.value())
                            .forEach(subtype -> addMapping(discriminator, subtype.name(), subtype.value(), context));
                }
            }

            private void addMapping(Discriminator discriminator, String name, Class<?> type, ModelConverterContext context) {
                boolean isNamed = name != null && !name.isBlank();
                String schemaName = context.resolve(new AnnotatedType().type(type)).getName();
                String ref = RefUtils.constructRef(schemaName);
                Map<String, String> mappings = Optional.ofNullable(discriminator.getMapping()).orElseGet(Map::of);

                if (!isNamed && mappings.containsValue(ref)) {
                    // Skip adding the unnamed mapping
                    return;
                }

                discriminator.mapping(isNamed ? name : schemaName, ref);

                if (isNamed && ref.equals(mappings.get(schemaName))) {
                    // Remove previous unnamed mapping
                    discriminator.getMapping().remove(schemaName);
                }
            }
        };
    }



}
