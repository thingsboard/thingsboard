// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration.template;

import org.thingsboard.server.common.data.SecretType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field on an integration's runtime configuration POJO as exposed-to-templating
 * during IoT Hub package export. The export service walks classes listed in
 * {@code IntegrationConfigPojoRegistry}; for each annotated field it replaces the value
 * at the corresponding JSON path with a {@code ${key}} placeholder and emits a matching
 * entry in the package's {@code form.json}. The install-time form renderer drives the
 * picker via {@code secretSupport}; the picker's output (plaintext or a
 * {@code ${secret:NAME;type:TYPE}} reference) is substituted in place of the placeholder
 * verbatim, and PE's {@code SecretConfigurationService} resolves any embedded references
 * at integration runtime.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateField {

    /** Form-field key. Used verbatim as the {@code ${key}} placeholder in
     *  {@code integration.json} and as the {@code key} property on the matching
     *  {@code form.json} entry. */
    String key();

    /** Form-field UI label. Empty falls through to the field name. */
    String label() default "";

    /** Form-field widget type for the generated form.json entry. */
    FormFieldType type() default FormFieldType.STRING;

    /** When true: emit {@code secretSupport=true} (and {@code secretType}) on the form
     *  entry so the install dialog renders the secret picker; the form-entry's
     *  {@code defaultValue} is forced to empty string. */
    boolean secret() default false;

    /** Secret content type. Only meaningful when secret=true. */
    SecretType secretType() default SecretType.TEXT;

    /** Optional rich help text shown below the form input. */
    String helpText() default "";

    /** Optional UI grouping label (rendered as a section header by the form renderer). */
    String group() default "";

    /** SELECT/STRING_AUTOCOMPLETE options. Ignored for other widget types. */
    String[] options() default {};

    /** Whether the form input is required. */
    boolean required() default false;

    /** Optional dotted JSON-path override (e.g. "credentials.token") when reflection-derived
     *  path doesn't match the persisted JSON layout. Empty = path derived from the field's
     *  position in the POJO graph. */
    String path() default "";
}
