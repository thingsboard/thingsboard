// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ai;

import org.thingsboard.server.common.data.ai.AiModel;

import static java.util.Objects.requireNonNull;
import static org.thingsboard.server.dao.ai.AiModelCacheEvictEvent.Deleted;
import static org.thingsboard.server.dao.ai.AiModelCacheEvictEvent.Saved;

sealed interface AiModelCacheEvictEvent permits Saved, Deleted {

    AiModelCacheKey cacheKey();

    record Saved(AiModelCacheKey cacheKey, AiModel savedModel) implements AiModelCacheEvictEvent {

        public Saved {
            requireNonNull(cacheKey);
            requireNonNull(savedModel);
        }

    }

    record Deleted(AiModelCacheKey cacheKey) implements AiModelCacheEvictEvent {

        public Deleted {
            requireNonNull(cacheKey);
        }

    }

}
