// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

public interface DeleteTsKvQuery extends TsKvQuery {

    Boolean getRewriteLatestIfDeleted();

    Boolean getDeleteLatest();

}
