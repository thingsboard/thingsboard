/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributesImmediateOutputStrategy implements AttributesOutputStrategy {

    private boolean sendAttributesUpdatedNotification;
    private boolean updateAttributesOnlyOnValueChange;

    private boolean saveAttribute;
    private boolean sendWsUpdate;
    private boolean processCfs;

    @Override
    public OutputStrategyType getType() {
        return OutputStrategyType.IMMEDIATE;
    }

    @Override
    public boolean hasContextOnlyChanges(OutputStrategy other) {
        if (!(other instanceof AttributesImmediateOutputStrategy otherStrategy)) {
            return true;
        }
        boolean saveTimeSeriesUpdated = saveAttribute != otherStrategy.isSaveAttribute();
        boolean sendWsUpdateUpdated = sendWsUpdate != otherStrategy.isSendWsUpdate();
        boolean processCfsUpdated = processCfs != otherStrategy.isProcessCfs();
        return saveTimeSeriesUpdated || sendWsUpdateUpdated || processCfsUpdated;
    }

    @Override
    public boolean hasRefreshContextOnlyChanges(OutputStrategy other) {
        if (!(other instanceof AttributesImmediateOutputStrategy otherStrategy)) {
            return true;
        }
        boolean updateAttrOnValueChangedChanged = updateAttributesOnlyOnValueChange != otherStrategy.isUpdateAttributesOnlyOnValueChange();
        boolean sendAttrUpdatedNotificationChanged = sendAttributesUpdatedNotification != otherStrategy.isSendAttributesUpdatedNotification();
        return updateAttrOnValueChangedChanged || sendAttrUpdatedNotificationChanged;
    }

}
