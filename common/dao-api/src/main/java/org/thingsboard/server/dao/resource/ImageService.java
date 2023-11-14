package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public interface ImageService {

    TbResourceInfo saveImage(TbResource image);

    TbResourceInfo saveImageInfo(TbResourceInfo imageInfo);

    TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key);

    PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, PageLink pageLink);

    byte[] getImageData(TenantId tenantId, TbResourceId imageId);

    byte[] getImagePreview(TenantId tenantId, TbResourceId imageId);

    String getImageLink(TbResourceInfo imageInfo);

}
