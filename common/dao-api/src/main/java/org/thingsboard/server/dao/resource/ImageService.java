// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;

import java.util.Collection;
import java.util.Set;

public interface ImageService {

    TbResourceInfo saveImage(TbResource image);

    TbResourceInfo saveImageInfo(TbResourceInfo imageInfo);

    TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key);

    Set<String> getAllImageKeysByTenantId(TenantId tenantId);

    TbResourceInfo getPublicImageInfoByKey(String publicResourceKey);

    PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, ResourceSubType imageSubType, PageLink pageLink);

    PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, ResourceSubType imageSubType, PageLink pageLink);

    byte[] getImageData(TenantId tenantId, TbResourceId imageId);

    byte[] getImagePreview(TenantId tenantId, TbResourceId imageId);

    ResourceExportData exportImage(TbResourceInfo imageInfo);

    TbResource toImage(TenantId tenantId, ResourceExportData imageData, boolean checkExisting);

    TbImageDeleteResult deleteImage(TbResourceInfo imageInfo, boolean force);

    String calculateImageEtag(byte[] imageData);

    TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, String etag);

    boolean replaceBase64WithImageUrl(HasImage entity, String type);

    boolean updateImagesUsage(Dashboard dashboard);

    boolean updateImagesUsage(WidgetTypeDetails widgetType);

    <T extends HasImage> T inlineImage(T entity);

    Collection<TbResourceInfo> getUsedImages(Dashboard dashboard);

    Collection<TbResourceInfo> getUsedImages(WidgetTypeDetails widgetTypeDetails);

    void inlineImageForEdge(HasImage entity);

    void inlineImagesForEdge(Dashboard dashboard);

    void inlineImagesForEdge(WidgetTypeDetails widgetTypeDetails);

    TbResourceInfo createOrUpdateSystemImage(String resourceKey, byte[] data);

}
