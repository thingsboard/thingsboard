// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import org.thingsboard.server.common.data.HasVersion;

/**
 * @author Andrew Shvayka
 */
public interface AttributeKvEntry extends KvEntry, HasVersion {

    long getLastUpdateTs();

}
