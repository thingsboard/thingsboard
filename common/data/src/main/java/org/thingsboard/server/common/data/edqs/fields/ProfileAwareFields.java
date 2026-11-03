// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import java.util.UUID;

public interface ProfileAwareFields extends EntityFields {

    String getProfileName();

    UUID getProfileId();

}
