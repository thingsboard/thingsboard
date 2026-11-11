// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.translation;

import lombok.Data;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;

@Data
public class CustomTranslationEvictEvent {
    private final CustomTranslationCompositeKey key;
}
