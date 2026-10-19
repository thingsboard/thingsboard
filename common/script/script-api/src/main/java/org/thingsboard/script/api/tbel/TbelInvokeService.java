// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.server.common.data.script.ScriptLanguage;

public interface TbelInvokeService extends ScriptInvokeService {

    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.TBEL;
    }

}
