// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class KeyFilter implements Serializable {

    private EntityKey key;
    private EntityKeyValueType valueType;
    private KeyFilterPredicate predicate;

}
