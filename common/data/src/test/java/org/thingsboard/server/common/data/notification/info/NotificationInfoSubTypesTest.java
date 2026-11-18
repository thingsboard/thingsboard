// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NotificationInfo} lists its implementations by hand in both {@code @JsonSubTypes} and the OpenAPI
 * {@code discriminatorMapping}. These tests make sure both lists match the classes that actually implement it.
 */
class NotificationInfoSubTypesTest {

    private static final String BASE_PACKAGE = "org.thingsboard.server.common.data.notification";

    @Test
    void jsonSubTypesCoverEveryImplementation() {
        Set<Class<?>> subTypes = Arrays.stream(NotificationInfo.class.getAnnotation(JsonSubTypes.class).value())
                .map(JsonSubTypes.Type::value)
                .collect(toSet());

        assertThat(subTypes)
                .as("@JsonSubTypes on NotificationInfo must list exactly the concrete NotificationInfo implementations")
                .isEqualTo(findImplementations());
    }

    @Test
    void discriminatorMappingCoversEveryImplementation() {
        Set<Class<?>> mappedSchemas = Arrays.stream(discriminatorMapping())
                .map(DiscriminatorMapping::schema)
                .collect(toSet());

        assertThat(mappedSchemas)
                .as("@Schema(discriminatorMapping) on NotificationInfo must list exactly the concrete NotificationInfo implementations")
                .isEqualTo(findImplementations());
    }

    @Test
    void discriminatorMappingValueIsFullyQualifiedClassName() {
        for (DiscriminatorMapping mapping : discriminatorMapping()) {
            assertThat(mapping.value())
                    .as("Discriminator value for %s must be its fully qualified class name (JsonTypeInfo.Id.CLASS)",
                            mapping.schema().getSimpleName())
                    .isEqualTo(mapping.schema().getName());
        }
    }

    private static DiscriminatorMapping[] discriminatorMapping() {
        return NotificationInfo.class.getAnnotation(Schema.class).discriminatorMapping();
    }

    private static Set<Class<?>> findImplementations() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(NotificationInfo.class));
        Set<Class<?>> implementations = scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .map(beanDefinition -> ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), null))
                .collect(toSet());
        assertThat(implementations).as("No NotificationInfo implementations found in %s", BASE_PACKAGE).isNotEmpty();
        return implementations;
    }
}
