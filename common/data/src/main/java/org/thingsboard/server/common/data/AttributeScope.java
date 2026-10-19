// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum AttributeScope {

    CLIENT_SCOPE(1),
    SERVER_SCOPE(2),
    SHARED_SCOPE(3);
    @Getter
    private final int id;

    private static final Map<Integer, AttributeScope> values = Arrays.stream(values())
            .collect(Collectors.toMap(AttributeScope::getId, scope -> scope));

    AttributeScope(int id) {
        this.id = id;
    }

    public static AttributeScope valueOf(int id) {
        return values.get(id);
    }

}
