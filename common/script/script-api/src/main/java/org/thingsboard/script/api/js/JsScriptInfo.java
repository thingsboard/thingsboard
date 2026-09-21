// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.js;

import lombok.Data;

@Data
public class JsScriptInfo {

    private final String hash;
    private final String functionName;

}
