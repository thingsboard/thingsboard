// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.asset.AssetProfile;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssetProfileDefinition extends AssetProfile {

    private String jsonId;

}
