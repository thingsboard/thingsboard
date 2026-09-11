// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.mapper;

import lombok.Data;

import java.util.Map;

@Data
public class AttributesResponse {
    private Map<String, Object> client;
    private Map<String, Object> shared;
}
