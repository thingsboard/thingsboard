// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.resource;

import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbImageService {

    TbResourceInfo save(TbResource image, User user) throws Exception;

    TbResourceInfo save(TbResourceInfo imageInfo, TbResourceInfo oldImageInfo, User user);

    TbImageDeleteResult delete(TbResourceInfo imageInfo, User user, boolean force);

    String getETag(ImageCacheKey imageCacheKey);

    void putETag(ImageCacheKey imageCacheKey, String etag);

    void evictETags(ImageCacheKey imageCacheKey);

    TbResourceInfo importImage(ResourceExportData imageData, boolean checkExisting, SecurityUser user) throws Exception;

}
