// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.sync.vc.request.create.AutoVersionCreateConfig;

import java.util.HashMap;

public class AutoCommitSettings extends HashMap<EntityType, AutoVersionCreateConfig> {

    private static final long serialVersionUID = -5757067601838792059L;

}
