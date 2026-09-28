// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.signup;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Created by igor on 12/13/16.
 */
@Schema
public enum SignUpResult {

    SUCCESS,
    INACTIVE_USER_EXISTS

}
