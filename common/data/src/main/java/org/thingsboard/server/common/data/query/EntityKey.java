// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class EntityKey implements Serializable {
    private static final long serialVersionUID = -6421575477523085543L;

    private final EntityKeyType type;
    private final String key;
}
