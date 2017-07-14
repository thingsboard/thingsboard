/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.nosql;


import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.model.SearchTextEntity;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = WIDGETS_BUNDLE_COLUMN_FAMILY_NAME)
public final class WidgetsBundleEntity implements SearchTextEntity<WidgetsBundle> {

    @Transient
    private static final long serialVersionUID = -8842195928585650849L;

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = WIDGETS_BUNDLE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = WIDGETS_BUNDLE_ALIAS_PROPERTY)
    private String alias;

    @Column(name = WIDGETS_BUNDLE_TITLE_PROPERTY)
    private String title;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = WIDGETS_BUNDLE_IMAGE_PROPERTY)
    private ByteBuffer image;

    public WidgetsBundleEntity() {
        super();
    }

    public WidgetsBundleEntity(WidgetsBundle widgetsBundle) {
        if (widgetsBundle.getId() != null) {
            this.id = widgetsBundle.getId().getId();
        }
        if (widgetsBundle.getTenantId() != null) {
            this.tenantId = widgetsBundle.getTenantId().getId();
        }
        this.alias = widgetsBundle.getAlias();
        this.title = widgetsBundle.getTitle();
        if (widgetsBundle.getImage() != null) {
            this.image = ByteBuffer.wrap(widgetsBundle.getImage());
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ByteBuffer getImage() {
        return image;
    }

    public void setImage(ByteBuffer image) {
        this.image = image;
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (searchText != null ? searchText.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WidgetsBundleEntity that = (WidgetsBundleEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (searchText != null ? !searchText.equals(that.searchText) : that.searchText != null) return false;
        return image != null ? image.equals(that.image) : that.image == null;

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WidgetsBundleEntity{");
        sb.append("id=").append(id);
        sb.append(", tenantId=").append(tenantId);
        sb.append(", alias='").append(alias).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", searchText='").append(searchText).append('\'');
        sb.append(", image=").append(image);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public WidgetsBundle toData() {
        WidgetsBundle widgetsBundle = new WidgetsBundle(new WidgetsBundleId(id));
        widgetsBundle.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            widgetsBundle.setTenantId(new TenantId(tenantId));
        }
        widgetsBundle.setAlias(alias);
        widgetsBundle.setTitle(title);
        if (image != null) {
            byte[] imageByteArray = new byte[image.remaining()];
            image.get(imageByteArray);
            widgetsBundle.setImage(imageByteArray);
        }
        return widgetsBundle;
    }
}
