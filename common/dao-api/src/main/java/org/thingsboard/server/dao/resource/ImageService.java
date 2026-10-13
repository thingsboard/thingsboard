// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.wl.WhiteLabeling;

import java.util.Collection;
import java.util.Set;

public interface ImageService {

    TbResourceInfo saveImage(TbResource image);

    TbResourceInfo saveImageInfo(TbResourceInfo imageInfo);

    TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key);

    TbResourceInfo getImageInfoByTenantIdAndCustomerIdAndKey(TenantId tenantId, CustomerId customerId, String key);

    Set<String> getAllImageKeysByTenantId(TenantId tenantId);

    TbResourceInfo getPublicImageInfoByKey(String publicResourceKey);

    PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, ResourceSubType imageSubType, PageLink pageLink);

    PageData<TbResourceInfo> getImagesByCustomerId(TenantId tenantId, CustomerId customerId, ResourceSubType imageSubType, PageLink pageLink);

    PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, ResourceSubType imageSubType, PageLink pageLink);

    byte[] getImageData(TenantId tenantId, TbResourceId imageId);

    byte[] getImagePreview(TenantId tenantId, TbResourceId imageId);

    ResourceExportData exportImage(TbResourceInfo imageInfo);

    TbResource toImage(TenantId tenantId, CustomerId customerId, ResourceExportData imageData, boolean checkExisting);

    TbImageDeleteResult deleteImage(TbResourceInfo imageInfo, boolean force);

    String calculateImageEtag(byte[] imageData);

    TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, String etag);

    TbResourceInfo findSystemOrCustomerImageByEtag(TenantId tenantId, CustomerId customerId, String etag);

    boolean replaceBase64WithImageUrl(HasImage entity, String type);

    boolean replaceBase64WithImageUrl(WhiteLabeling whiteLabeling);

    boolean updateImagesUsage(Dashboard dashboard);

    boolean updateImagesUsage(WidgetTypeDetails widgetType);

    <T extends HasImage> T inlineImage(T entity);

    Collection<TbResourceInfo> getUsedImages(Dashboard dashboard);

    Collection<TbResourceInfo> getUsedImages(WidgetTypeDetails widgetTypeDetails);

    void inlineImageForEdge(HasImage entity);

    void inlineImagesForEdge(Dashboard dashboard);

    void inlineImagesForEdge(WidgetTypeDetails widgetTypeDetails);

    void inlineImagesForEdge(TenantId tenantId, JsonNode settings);

    TbResourceInfo createOrUpdateSystemImage(String resourceKey, byte[] data);

}
