// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.js;

import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.server.common.data.script.ScriptLanguage;

public interface JsInvokeService extends ScriptInvokeService {

    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.JS;
    }

}
