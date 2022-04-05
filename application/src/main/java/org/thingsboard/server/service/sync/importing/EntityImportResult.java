/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.importing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.utils.ThrowingRunnable;

@Data
public class EntityImportResult<E extends ExportableEntity<? extends EntityId>> {
    private E savedEntity;
    private E oldEntity;
    @JsonIgnore
    private transient ThrowingRunnable callback; // to call when entity is successfully saved and transaction is committed
}
