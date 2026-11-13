// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

public interface TsKvQuery {

    int getId();

    String getKey();

    long getStartTs();

    long getEndTs();

}
