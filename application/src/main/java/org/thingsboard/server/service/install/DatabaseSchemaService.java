// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

public interface DatabaseSchemaService {

    void createDatabaseSchema() throws Exception;

    void createDatabaseSchema(boolean createIndexes) throws Exception;

    void createDatabaseIndexes() throws Exception;

}
