// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.thingsboard.server.common.data.ObjectType;

public interface EdqsObject {

    @JsonIgnore
    String stringKey();

    @JsonIgnore
    Long version();

    @JsonIgnore
    ObjectType type();

}
