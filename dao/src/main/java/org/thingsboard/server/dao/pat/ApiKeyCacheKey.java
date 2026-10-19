// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.pat;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

record ApiKeyCacheKey(String value) implements Serializable {

    ApiKeyCacheKey {
        requireNonNull(value);
    }

    static ApiKeyCacheKey of(String value) {
        return new ApiKeyCacheKey(value);
    }

    @NonNull
    @Override
    public String toString() {
        return /* cache name */ "_" + value;
    }

}
