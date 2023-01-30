/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.rule.TbAlarmRuleService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_RULE_ID;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_RULE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_RULE_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_RULE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmRuleController extends BaseController {

    private final TbAlarmRuleService tbAlarmRuleService;

    public static final String ALARM_RULE_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', " +
            "the server checks that the alarm rule is owned by the same tenant.";

    @ApiOperation(value = "Get AlarmRule (getAlarmRuleById)",
            notes = "Get the AlarmRule object based on the provided AlarmRule Id. "
                    + ALARM_RULE_SECURITY_CHECK + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/alarmRule/{alarmRuleId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmRule getAlarmRuleById(@ApiParam(value = ALARM_RULE_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ALARM_RULE_ID) String strAlarmRuleId) throws ThingsboardException {
        checkParameter(ALARM_RULE_ID, strAlarmRuleId);
        AlarmRuleId alarmRuleId = new AlarmRuleId(toUUID(strAlarmRuleId));
        return checkAlarmRuleId(alarmRuleId, Operation.READ);
    }

    @ApiOperation(value = "Create or update AlarmRule (saveAlarmRule)",
            notes = "Creates or Updates the AlarmRule. When creating alarm rule, platform generates AlarmRule Id as " + UUID_WIKI_LINK +
                    "The newly created AlarmRule Id will be present in the response. " +
                    "Specify existing AlarmRule Id to update the AlarmRule. " +
                    "Referencing non-existing AlarmRule Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new AlarmRule entity. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/alarmRule", method = RequestMethod.POST)
    @ResponseBody
    public AlarmRule saveAlarmRule(@ApiParam(value = "A JSON value representing the alarm rule.") @RequestBody AlarmRule alarmRule) throws Exception {
        alarmRule.setTenantId(getTenantId());
        checkEntity(alarmRule.getId(), alarmRule, Resource.ALARM_RULE);
        return tbAlarmRuleService.save(alarmRule, getCurrentUser());
    }

    @ApiOperation(value = "Delete AlarmRule (deleteAlarmRule)", notes = "Deletes the AlarmRule. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/alarmRule/{alarmRuleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAlarmRule(@ApiParam(value = ALARM_RULE_ID_PARAM_DESCRIPTION)
                                @PathVariable(ALARM_RULE_ID) String strAlarmRuleId) throws ThingsboardException {
        checkParameter(ALARM_RULE_ID, strAlarmRuleId);
        AlarmRuleId alarmRuleId = new AlarmRuleId(toUUID(strAlarmRuleId));
        AlarmRule alarmRule = checkAlarmRuleId(alarmRuleId, Operation.DELETE);
        tbAlarmRuleService.delete(alarmRule, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant AlarmRuleInfos (getAlarmRuleInfos)",
            notes = "Returns a page of alarm rule infos owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/alarmRuleInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmRuleInfo> getAlarmRuleInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_RULE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_RULE_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(alarmRuleService.findAlarmRuleInfos(tenantId, pageLink));
    }
}
