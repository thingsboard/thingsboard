// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Component
public class PojoFieldWalker {

    /** One annotated field discovered during a walk. */
    public record WalkedField(Field field, TemplateField annotation, String jsonPath) {}

    /**
     * Walk {@code rootClass} (declared fields plus inherited via {@link Class#getSuperclass()}),
     * discovering every {@link TemplateField}-annotated field. For each, emit a
     * {@link WalkedField} whose {@code jsonPath} is the dotted path from {@code pathPrefix} to
     * the field's position (or to the explicit {@link TemplateField#path()} override when set).
     *
     * <p>Annotated leaves are not walked into. For unannotated fields, two descent rules apply:
     * <ul>
     *   <li><b>Polymorphic:</b> declared type carries {@code @JsonTypeInfo + @JsonSubTypes} →
     *       walk only the subtype matching the persisted discriminator value.</li>
     *   <li><b>Plain composition:</b> declared type itself contains at least one
     *       {@code @TemplateField}-annotated field → walk that type unconditionally. Bounded to
     *       types that actually carry our annotation, so stdlib/framework types are not visited.</li>
     * </ul>
     */
    public List<WalkedField> walk(Class<?> rootClass, JsonNode persistedJson, String pathPrefix) {
        List<WalkedField> out = new ArrayList<>();
        walkInto(rootClass, persistedJson, pathPrefix, out);
        return out;
    }

    private void walkInto(Class<?> clazz, JsonNode currentJson, String prefix, List<WalkedField> out) {
        if (clazz == null || clazz == Object.class) return;
        for (Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            TemplateField tf = f.getAnnotation(TemplateField.class);
            String fieldName = f.getName();
            String pathToField = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;

            if (tf != null) {
                String jsonPath = tf.path().isEmpty() ? pathToField : (
                        prefix.isEmpty() ? tf.path() : prefix + "." + tf.path()
                );
                out.add(new WalkedField(f, tf, jsonPath));
                continue;       // annotated leaves are not walked into
            }

            // Unannotated field. Two descent paths:
            //   (1) Polymorphic: declared type carries @JsonTypeInfo + @JsonSubTypes → walk
            //       only the subtype matching the persisted discriminator.
            //   (2) Plain composition: declared type itself contains at least one
            //       @TemplateField-annotated field → walk that type unconditionally.
            Class<?> declared = f.getType();
            com.fasterxml.jackson.annotation.JsonTypeInfo typeInfo =
                declared.getAnnotation(com.fasterxml.jackson.annotation.JsonTypeInfo.class);
            com.fasterxml.jackson.annotation.JsonSubTypes subTypes =
                declared.getAnnotation(com.fasterxml.jackson.annotation.JsonSubTypes.class);
            JsonNode childJson = currentJson != null ? currentJson.get(fieldName) : null;

            if (typeInfo != null && subTypes != null) {
                if (childJson == null || childJson.isMissingNode() || childJson.isNull()) {
                    continue;
                }
                JsonNode discriminator = childJson.get(typeInfo.property());
                if (discriminator == null || !discriminator.isTextual()) {
                    continue;
                }
                String discriminatorValue = discriminator.asText();
                for (com.fasterxml.jackson.annotation.JsonSubTypes.Type sub : subTypes.value()) {
                    if (sub.name().equals(discriminatorValue)) {
                        walkInto(sub.value(), childJson, pathToField, out);
                        break;
                    }
                }
            } else if (hasTemplateFieldsAnywhere(declared)) {
                walkInto(declared, childJson, pathToField, out);
            }
        }
        // Continue with superclass (handles inheritance hierarchies).
        walkInto(clazz.getSuperclass(), currentJson, prefix, out);
    }

    /** True if {@code clazz} or any of its superclasses declares a {@link TemplateField}-annotated
     *  non-static field. Bounds the plain-composition descent so the walker doesn't recurse into
     *  unrelated stdlib / framework types. */
    private static boolean hasTemplateFieldsAnywhere(Class<?> clazz) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getAnnotation(TemplateField.class) != null) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
