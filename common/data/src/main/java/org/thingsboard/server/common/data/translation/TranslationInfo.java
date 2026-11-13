// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.translation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Slf4j
public class TranslationInfo implements Serializable {

    @Schema(description = "Locale code formed by combining the ISO 639-1 language code and the ISO 3166-1 region code. For example, \"en_US\"")
    private String localeCode;
    @Schema(description = "Locale code language display name. For example, \"Polish (Polski)\"")
    private String language;
    @Schema(description = "Locale code country display name. For example, \"Poland\"")
    private String country;
    @Schema(description = "Number representing translation percentage progress. For example, 40 that means 40% of all keys are translated.")
    private int progress;
    @Schema(description = "Boolean representing if current language has customization.")
    private boolean customized;

}
