/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.event;

import lombok.Data;
import org.eclipse.leshan.core.util.StringUtils;

@Data
public abstract class DebugEvent implements EventFilter {

    private String msgDirectionType;
    private String server;
    private String dataSearch;
    private String metadataSearch;
    private String entityName;
    private String relationType;
    private String entityId;
    private String msgType;
    private boolean isError;
    private String error;

    public void setIsError(boolean isError) {
        this.isError = isError;
    }

    @Override
    public boolean hasFilterForJsonBody() {
        return !StringUtils.isEmpty(msgDirectionType) || !StringUtils.isEmpty(server) || !StringUtils.isEmpty(dataSearch) || !StringUtils.isEmpty(metadataSearch)
                || !StringUtils.isEmpty(entityName) || !StringUtils.isEmpty(relationType) || !StringUtils.isEmpty(entityId) || !StringUtils.isEmpty(msgType) || !StringUtils.isEmpty(error) || isError;
    }

}
