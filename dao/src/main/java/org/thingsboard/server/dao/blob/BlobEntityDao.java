// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.blob;

import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

/**
 * The Interface BlobEntityDao.
 *
 */
public interface BlobEntityDao extends Dao<BlobEntity>, TenantEntityDao<BlobEntity> {

    void cleanUpBlobEntities(long expTime);

    void migrateBlobEntities();

}
