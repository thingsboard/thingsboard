// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.mobile.secret;

import lombok.Data;

@Data
public class MobileSecretEvictEvent {

    private final String secret;

}
