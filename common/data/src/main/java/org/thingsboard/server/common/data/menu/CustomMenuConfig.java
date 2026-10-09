// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Views;

import java.util.ArrayList;
import java.util.List;

@Schema
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class CustomMenuConfig {

    @ArraySchema(schema = @Schema(implementation = MenuItem.class))
    @JsonView(Views.Public.class)
    @Valid
    private List<MenuItem> items = new ArrayList<>();

}
