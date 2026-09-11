// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.layout;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Views;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomMobilePage extends AbstractMobilePage {

    @Schema(description = "Path to custom page", example = "/alarmDetails/868c7083-032d-4f52-b8b4-7859aebb6a4e")
    @JsonView(Views.Public.class)
    private String path;

    @Override
    public MobilePageType getType() {
        return MobilePageType.CUSTOM;
    }
}
