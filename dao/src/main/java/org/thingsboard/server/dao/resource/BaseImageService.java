package org.thingsboard.server.dao.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;

import java.util.Set;

@Service
public class BaseImageService extends BaseResourceService implements ImageService {

    public BaseImageService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao, ResourceDataValidator resourceValidator) {
        super(resourceDao, resourceInfoDao, resourceValidator);
    }

    @Override
    public TbResourceInfo saveImage(TbResource image) {
        resourceValidator.validate(image, TbResourceInfo::getTenantId);
        if (image.getData() != null) {

        }
        // generate preview, etc.
        return saveResource(image, false);
    }

    @Override
    public TbResourceInfo saveImageInfo(TbResourceInfo imageInfo) {
        return null;
    }

    @Override
    public TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key) {
        return findResourceInfoByTenantIdAndKey(tenantId, ResourceType.IMAGE, key);
    }

    @Override
    public PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, PageLink pageLink) {
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .resourceTypes(Set.of(ResourceType.IMAGE))
                .build();
        return findTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, PageLink pageLink) {
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .resourceTypes(Set.of(ResourceType.IMAGE))
                .build();
        return findAllTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public byte[] getImageData(TenantId tenantId, TbResourceId imageId) {
        return resourceDao.getResourceData(tenantId, imageId);
    }

    @Override
    public byte[] getImagePreview(TenantId tenantId, TbResourceId imageId) {
        return new byte[0];
    }

    @Override
    public String getImageLink(TbResourceInfo imageInfo) {
        String link = "/api/images/";
        if (imageInfo.getTenantId().isSysTenantId()) {
            link += "system/";
        } else {
            link += "tenant/";
        }
        link += imageInfo.getResourceKey();
        return link;
    }

}
