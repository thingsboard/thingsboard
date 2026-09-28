// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.blob;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;

public interface TbBlobService {

    void delete(BlobEntityInfo blobEntityInfo, User user);

}
