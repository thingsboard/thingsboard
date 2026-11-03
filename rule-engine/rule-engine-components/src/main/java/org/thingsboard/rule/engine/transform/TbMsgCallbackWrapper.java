// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

public interface TbMsgCallbackWrapper {

    void onSuccess();

    void onFailure(Throwable t);
}
