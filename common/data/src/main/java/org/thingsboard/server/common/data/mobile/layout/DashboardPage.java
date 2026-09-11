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
public class DashboardPage extends AbstractMobilePage {

    @Schema(description = "Dashboard id", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    @JsonView(Views.Public.class)
    private String dashboardId;

    @Override
    public MobilePageType getType() {
        return MobilePageType.DASHBOARD;
    }
}
