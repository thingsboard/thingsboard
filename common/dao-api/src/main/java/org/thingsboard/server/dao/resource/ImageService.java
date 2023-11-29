/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;

import java.util.List;

public interface ImageService {

    TbResourceInfo saveImage(TbResource image);

    TbResourceInfo saveImageInfo(TbResourceInfo imageInfo);

    TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key);

    PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, PageLink pageLink);

    byte[] getImageData(TenantId tenantId, TbResourceId imageId);

    byte[] getImagePreview(TenantId tenantId, TbResourceId imageId);

    TbImageDeleteResult deleteImage(TbResourceInfo imageInfo, boolean force);

    List<TbResourceInfo> findSimilarImagesByTenantIdAndKeyStartingWith(TenantId tenantId, byte[] data, String imageKeyStartingWith);

    TbResourceInfo findImageByTenantIdAndEtag(TenantId tenantId, String etag);

    boolean replaceBase64WithImageUrl(HasImage hasImage, String title, String type);
    boolean replaceBase64WithImageUrl(Dashboard dashboard);
    boolean replaceBase64WithImageUrl(WidgetTypeDetails widgetType);

    void inlineImage(HasImage entity);

    void inlineImages(Dashboard dashboard);

    void inlineImages(WidgetTypeDetails widgetTypeDetails);
}
