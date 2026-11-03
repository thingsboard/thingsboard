// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device.claim;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class ClaimData implements Serializable {

    private static final long serialVersionUID = -3922621193389915930L;

    private final String secretKey;
    private final long expirationTime;

}
