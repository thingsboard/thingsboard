// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.CUSTOM_MENU_TABLE_NAME)
public class CustomMenuEntity extends AbstractCustomMenuEntity<CustomMenu> {

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CUSTOM_MENU_CONFIG)
    private JsonNode config;

    public CustomMenuEntity() {
        super();
    }

    public CustomMenuEntity(CustomMenu customMenu) {
        super(customMenu);
        this.config = toJson(customMenu.getConfig());
    }

    @Override
    public CustomMenu toData() {
        return new CustomMenu(super.toCustomMenuInfo(), fromJson(config, CustomMenuConfig.class));
    }

}
