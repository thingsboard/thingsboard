// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

@Component
@Profile("install")
public class DbUpgradeExecutorService extends DbCallbackExecutorService {

}
