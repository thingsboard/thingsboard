// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device.claim;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.Device;

@AllArgsConstructor
@Data
public class ClaimResult {

    private Device device;
    private ClaimResponse response;

}
