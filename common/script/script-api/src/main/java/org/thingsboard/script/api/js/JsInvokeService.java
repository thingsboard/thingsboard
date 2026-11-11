// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.script.api.js;

import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.server.common.data.script.ScriptLanguage;

public interface JsInvokeService extends ScriptInvokeService {

    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.JS;
    }
    
}
