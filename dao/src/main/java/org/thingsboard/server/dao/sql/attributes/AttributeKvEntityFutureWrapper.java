/**
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.attributes;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Getter;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

public final class AttributeKvEntityFutureWrapper {
    @Getter
    private final SettableFuture<Void> future;
    @Getter
    private final AttributeKvEntity entity;

    private AttributeKvEntityFutureWrapper(SettableFuture<Void> future, AttributeKvEntity entity) {
        this.future = future;
        this.entity = entity;
    }

    public static AttributeKvEntityFutureWrapper create(SettableFuture<Void> future, AttributeKvEntity entity) {
        return new AttributeKvEntityFutureWrapper(future, entity);
    }
}


