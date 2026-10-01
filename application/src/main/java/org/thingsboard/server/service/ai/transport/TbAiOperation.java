// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.thingsboard.ai.common.client.TbAiClient;

import java.util.function.Supplier;

public record TbAiOperation(String type, Object payload, Supplier<TbAiClient.TbAiResponse> httpCall) {}
