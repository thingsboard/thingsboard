/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
