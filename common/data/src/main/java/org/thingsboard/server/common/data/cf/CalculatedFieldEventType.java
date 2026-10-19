// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf;

public enum CalculatedFieldEventType {

    INITIALIZED,
    UPDATED,

    TENANT_PROFILE_UPDATED,
    OWNER_CHANGED,
    RELATION_ADD_OR_UPDATE,
    RELATION_DELETED,

    REEVALUATION_MSG

}
