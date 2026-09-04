// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import org.thingsboard.server.common.data.edqs.fields.ProfileAwareFields;

import java.util.UUID;

public abstract class ProfileAwareData<T> extends BaseEntityData<ProfileAwareFields> {

    public ProfileAwareData(UUID id) {
        super(id);
    }

}
