// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "HOME", schema = HomeMenuItem.class),
                @DiscriminatorMapping(value = "DEFAULT", schema = DefaultMenuItem.class),
                @DiscriminatorMapping(value = "CUSTOM", schema = CustomMenuItem.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HomeMenuItem.class, name = "HOME"),
        @JsonSubTypes.Type(value = DefaultMenuItem.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = CustomMenuItem.class, name = "CUSTOM")
})
public interface MenuItem extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Menu item type")
    MenuItemType getType();

    boolean isVisible();

}
