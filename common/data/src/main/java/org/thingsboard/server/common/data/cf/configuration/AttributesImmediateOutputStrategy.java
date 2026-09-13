// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
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
