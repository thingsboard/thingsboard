// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.integration.api.IntegrationControllerApi;

public class BaseIntegrationController {

    @Autowired(required = false)
    protected IntegrationControllerApi api;

}
