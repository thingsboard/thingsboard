// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.event;

import java.io.Serializable;

public abstract class UserAuthDataChangedEvent implements Serializable {
    public abstract String getId();
    public abstract long getTs();
}
