// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.dao.Dao;

public interface OAuth2ParamsDao extends Dao<OAuth2Params> {
    void deleteAll();
}
