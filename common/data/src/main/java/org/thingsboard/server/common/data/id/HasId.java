// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import java.io.Serializable;

public interface HasId<I extends HasUUID> extends Serializable {

    I getId();

}
