// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Created by ashvayka on 07.06.18.
 */

public interface TbIntervalState {

    void update(JsonElement value);

    boolean hasChangesToPersist();

    void clearChangesToPersist();

    boolean hasChangesToReport();

    void clearChangesToReport();

    String toValueJson(Gson gson, String outputValueKey);

    String toStateJson(Gson gson);

}
