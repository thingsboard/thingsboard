// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sqlts.insert.latest;

import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;

import java.util.List;

public interface InsertLatestTsRepository {

    List<Long> saveOrUpdate(List<TsKvLatestEntity> entities);

}
