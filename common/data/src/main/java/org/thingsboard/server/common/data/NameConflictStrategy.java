// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record NameConflictStrategy(NameConflictPolicy policy, String separator, UniquifyStrategy uniquifyStrategy) {

    public static final NameConflictStrategy DEFAULT = new NameConflictStrategy(NameConflictPolicy.FAIL, null, null);

}
