/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.controller.cusomize.StateSystemDTO;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api/device/")
@RequiredArgsConstructor
@Slf4j
public class DeviceSettingController extends BaseController {

    // get current state of assets detail
    @ApiOperation(value = "Get current state of assets detail")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/state-system")
    public StateSystemDTO getStateChain() {
        return new StateSystemDTO(10, 100, 50, 5);
    }

}
