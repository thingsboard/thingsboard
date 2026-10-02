// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

/**
 * A typed channel operation: its frame type (a {@code ChannelProtocol} constant) and its request payload DTO, or null.
 */
public record TbAiOperation(String type, Object payload) {}
