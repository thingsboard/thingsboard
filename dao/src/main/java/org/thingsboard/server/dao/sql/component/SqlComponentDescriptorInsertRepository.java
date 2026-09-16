// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.component;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

@Repository
public class SqlComponentDescriptorInsertRepository extends AbstractComponentDescriptorInsertRepository {

    private static final String ID = "id = :id";
    private static final String CLAZZ_CLAZZ = "clazz = :clazz";

    private static final String P_KEY_CONFLICT_STATEMENT = "(id)";
    private static final String UNQ_KEY_CONFLICT_STATEMENT = "(clazz)";

    private static final String ON_P_KEY_CONFLICT_UPDATE_STATEMENT = getUpdateStatement(CLAZZ_CLAZZ);
    private static final String ON_UNQ_KEY_CONFLICT_UPDATE_STATEMENT = getUpdateStatement(ID);

    private static final String INSERT_OR_UPDATE_ON_P_KEY_CONFLICT = getInsertOrUpdateStatement(P_KEY_CONFLICT_STATEMENT, ON_P_KEY_CONFLICT_UPDATE_STATEMENT);
    private static final String INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT = getInsertOrUpdateStatement(UNQ_KEY_CONFLICT_STATEMENT, ON_UNQ_KEY_CONFLICT_UPDATE_STATEMENT);

    @Override
    public ComponentDescriptorEntity saveOrUpdate(ComponentDescriptorEntity entity) {
        return saveAndGet(entity, INSERT_OR_UPDATE_ON_P_KEY_CONFLICT, INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT);
    }

    @Override
    protected ComponentDescriptorEntity doProcessSaveOrUpdate(ComponentDescriptorEntity entity, String query) {
        return (ComponentDescriptorEntity) getQuery(entity, query).getSingleResult();
    }

    private static String getInsertOrUpdateStatement(String conflictKeyStatement, String updateKeyStatement) {
        return "INSERT INTO component_descriptor (id, created_time, actions, clazz, configuration_descriptor, configuration_version, name, scope, type, clustering_mode, has_queue_name) VALUES (:id, :created_time, :actions, :clazz, :configuration_descriptor, :configuration_version, :name, :scope, :type, :clustering_mode, :has_queue_name) ON CONFLICT " + conflictKeyStatement + " DO UPDATE SET " + updateKeyStatement + " returning *";
    }

    private static String getUpdateStatement(String id) {
        return "actions = :actions, " + id + ",created_time = :created_time, configuration_descriptor = :configuration_descriptor, configuration_version = :configuration_version, name = :name, scope = :scope, type = :type, clustering_mode = :clustering_mode, has_queue_name = :has_queue_name";
    }
}
