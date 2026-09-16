// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import java.util.Collections;
import java.util.List;

public record AttributesSaveResult(List<Long> versions) {

    public static final AttributesSaveResult EMPTY = new AttributesSaveResult(Collections.emptyList());

    public static AttributesSaveResult of(List<Long> versions) {
        if (versions == null) {
            return EMPTY;
        }
        return new AttributesSaveResult(versions);
    }

}
