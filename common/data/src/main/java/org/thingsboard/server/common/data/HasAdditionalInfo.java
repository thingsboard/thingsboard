// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;

public interface HasAdditionalInfo {

    JsonNode getAdditionalInfo();

}
