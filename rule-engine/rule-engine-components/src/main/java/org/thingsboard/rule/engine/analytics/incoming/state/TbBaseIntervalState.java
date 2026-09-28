// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.JsonElement;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
abstract class TbBaseIntervalState implements TbIntervalState {

    private boolean hasChangesToPersist = true;
    private boolean hasChangesToReport = true;

    @Override
    public void update(JsonElement value) {
        if(doUpdate(value)){
            hasChangesToPersist = true;
            hasChangesToReport = true;
        }
    }

    @Override
    public boolean hasChangesToReport(){
        return hasChangesToReport;
    }

    @Override
    public boolean hasChangesToPersist(){
        return hasChangesToPersist;
    }

    @Override
    public void clearChangesToPersist(){
        hasChangesToPersist = false;
    }

    @Override
    public void clearChangesToReport(){
        hasChangesToReport = false;
    }

    protected abstract boolean doUpdate(JsonElement value);
}
