// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.util;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class TuyaToken implements Serializable {
    private String accessToken;
    private String refreshToken;
    private String uid;
    private Long expireAt;
}
