// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.relation;

import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.List;

public interface RelationInsertRepository {

    RelationEntity saveOrUpdate(RelationEntity entity);

    List<RelationEntity> saveOrUpdate(List<RelationEntity> entities);

}