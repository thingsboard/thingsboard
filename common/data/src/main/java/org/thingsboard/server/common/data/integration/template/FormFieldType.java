// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration.template;

public enum FormFieldType {
    STRING,
    PASSWORD,
    INTEGER,
    BOOLEAN,
    SELECT,
    /** Free-text input with autocomplete suggestions (when expected values are well-known
     *  but extensible — e.g. LORIOT regional servers). Uses `options` as suggestions. */
    STRING_AUTOCOMPLETE
}
