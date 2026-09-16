// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
@Data
public class AttributeKey implements Serializable {
    private final String scope;
    private final String attributeKey;
}
