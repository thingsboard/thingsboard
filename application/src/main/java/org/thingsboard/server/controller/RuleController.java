/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RuleController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}", method = RequestMethod.GET)
    @ResponseBody
    public RuleMetaData getRuleById(@PathVariable("ruleId") String strRuleId) throws ThingsboardException {
        checkParameter("ruleId", strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            return checkRule(ruleService.findRuleById(ruleId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/token/{pluginToken}", method = RequestMethod.GET)
    @ResponseBody
    public List<RuleMetaData> getRulesByPluginToken(@PathVariable("pluginToken") String pluginToken) throws ThingsboardException {
        checkParameter("pluginToken", pluginToken);
        try {
            PluginMetaData plugin = checkPlugin(pluginService.findPluginByApiToken(pluginToken));
            return ruleService.findPluginRules(plugin.getApiToken());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule", method = RequestMethod.POST)
    @ResponseBody
    public RuleMetaData saveRule(@RequestBody RuleMetaData source) throws ThingsboardException {
        try {
            boolean created = source.getId() == null;
            source.setTenantId(getCurrentUser().getTenantId());
            RuleMetaData rule = checkNotNull(ruleService.saveRule(source));
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return rule;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void activateRuleById(@PathVariable("ruleId") String strRuleId) throws ThingsboardException {
        checkParameter("ruleId", strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            RuleMetaData rule = checkRule(ruleService.findRuleById(ruleId));
            ruleService.activateRuleById(ruleId);
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(), ComponentLifecycleEvent.ACTIVATED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}/suspend", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void suspendRuleById(@PathVariable("ruleId") String strRuleId) throws ThingsboardException {
        checkParameter("ruleId", strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            RuleMetaData rule = checkRule(ruleService.findRuleById(ruleId));
            ruleService.suspendRuleById(ruleId);
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(), ComponentLifecycleEvent.SUSPENDED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/rule/system", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<RuleMetaData> getSystemRules(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(ruleService.findSystemRules(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/rule/tenant/{tenantId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<RuleMetaData> getTenantRules(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(ruleService.findTenantRules(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rules", method = RequestMethod.GET)
    @ResponseBody
    public List<RuleMetaData> getRules() throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                return checkNotNull(ruleService.findSystemRules());
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                return checkNotNull(ruleService.findAllTenantRulesByTenantId(tenantId));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/rule", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<RuleMetaData> getTenantRules(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(ruleService.findTenantRules(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRule(@PathVariable("ruleId") String strRuleId) throws ThingsboardException {
        checkParameter("ruleId", strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            RuleMetaData rule = checkRule(ruleService.findRuleById(ruleId));
            ruleService.deleteRuleById(ruleId);
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(), ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
