// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.model.token;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.UserId;

public record ApiKeyAuthRequest(String apiKey, UserId userId, CustomerId customerId) {}
