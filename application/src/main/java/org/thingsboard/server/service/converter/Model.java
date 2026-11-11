// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import com.fasterxml.jackson.databind.JsonNode;

public record Model(String name, JsonNode info, String photo) {}
