/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public final class WidgetTypeInfoEntity extends AbstractWidgetTypeEntity<WidgetTypeInfo> {

    private String image;
    private String description;
    private String widgetType;

    public WidgetTypeInfoEntity() {
        super();
    }

    public WidgetTypeInfoEntity(WidgetTypeDetailsEntity widgetTypeDetailsEntity) {
        super(widgetTypeDetailsEntity);
        this.image = widgetTypeDetailsEntity.getImage();
        this.description = widgetTypeDetailsEntity.getDescription();
        if (widgetTypeDetailsEntity.getDescriptor() != null && widgetTypeDetailsEntity.getDescriptor().has("type")) {
            this.widgetType = widgetTypeDetailsEntity.getDescriptor().get("type").asText();
        } else {
            this.widgetType = "";
        }
    }

    @Override
    public WidgetTypeInfo toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetTypeInfo widgetTypeInfo = new WidgetTypeInfo(baseWidgetType);
        widgetTypeInfo.setImage(image);
        widgetTypeInfo.setDescription(description);
        widgetTypeInfo.setWidgetType(widgetType);
        return widgetTypeInfo;
    }

}
