// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import org.apache.commons.lang3.StringUtils;

public class VcUtils {

    private VcUtils() {}

    public static void checkBranchName(String branch) {
        if (StringUtils.isEmpty(branch)) return;

        boolean invalid = StringUtils.containsWhitespace(branch) ||
                StringUtils.containsAny(branch, "..", "~", "^", ":", "\\") ||
                StringUtils.endsWithAny(branch, "/", ".lock");
        if (invalid) {
            throw new IllegalArgumentException("Branch name is invalid");
        }
    }

}
