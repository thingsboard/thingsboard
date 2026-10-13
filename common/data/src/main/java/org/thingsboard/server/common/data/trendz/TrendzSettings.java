// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import java.io.Serializable;

public record TrendzSettings(TrendzConfiguration configuration,
                             TrendzSynchronizationResult synchronizationResult) implements Serializable {}
