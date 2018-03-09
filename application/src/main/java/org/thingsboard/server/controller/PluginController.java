/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PluginController extends BaseController {

    public static final String PLUGIN_ID = "pluginId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}", method = RequestMethod.GET)
    @ResponseBody
    public PluginMetaData getPluginById(@PathVariable(PLUGIN_ID) String strPluginId) throws ThingsboardException {
        checkParameter(PLUGIN_ID, strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            return checkPlugin(pluginService.findPluginById(pluginId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/token/{pluginToken}", method = RequestMethod.GET)
    @ResponseBody
    public PluginMetaData getPluginByToken(@PathVariable("pluginToken") String pluginToken) throws ThingsboardException {
        checkParameter("pluginToken", pluginToken);
        try {
            return checkPlugin(pluginService.findPluginByApiToken(pluginToken));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin", method = RequestMethod.POST)
    @ResponseBody
    public PluginMetaData savePlugin(@RequestBody PluginMetaData source) throws ThingsboardException {
        try {
            boolean created = source.getId() == null;
            source.setTenantId(getCurrentUser().getTenantId());
            PluginMetaData plugin = checkNotNull(pluginService.savePlugin(source));
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(plugin.getId(), plugin,
                    null,
                    created ? ActionType.ADDED : ActionType.UPDATED, null);

            return plugin;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.PLUGIN), source,
                    null, source.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void activatePluginById(@PathVariable(PLUGIN_ID) String strPluginId) throws ThingsboardException {
        checkParameter(PLUGIN_ID, strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            PluginMetaData plugin = checkPlugin(pluginService.findPluginById(pluginId));
            pluginService.activatePluginById(pluginId);
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(), ComponentLifecycleEvent.ACTIVATED);

            logEntityAction(plugin.getId(), plugin,
                    null,
                    ActionType.ACTIVATED, null, strPluginId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.PLUGIN),
                    null,
                    null,
                    ActionType.ACTIVATED, e, strPluginId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}/suspend", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void suspendPluginById(@PathVariable(PLUGIN_ID) String strPluginId) throws ThingsboardException {
        checkParameter(PLUGIN_ID, strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            PluginMetaData plugin = checkPlugin(pluginService.findPluginById(pluginId));
            pluginService.suspendPluginById(pluginId);
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(), ComponentLifecycleEvent.SUSPENDED);

            logEntityAction(plugin.getId(), plugin,
                    null,
                    ActionType.SUSPENDED, null, strPluginId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.PLUGIN),
                    null,
                    null,
                    ActionType.SUSPENDED, e, strPluginId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/plugin/system", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<PluginMetaData> getSystemPlugins(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(pluginService.findSystemPlugins(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/plugin/tenant/{tenantId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<PluginMetaData> getTenantPlugins(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(pluginService.findTenantPlugins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugins", method = RequestMethod.GET)
    @ResponseBody
    public List<PluginMetaData> getPlugins() throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                return checkNotNull(pluginService.findSystemPlugins());
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                List<PluginMetaData> plugins = checkNotNull(pluginService.findAllTenantPluginsByTenantId(tenantId));
                plugins.stream()
                        .filter(plugin -> plugin.getTenantId().getId().equals(ModelConstants.NULL_UUID))
                        .forEach(plugin -> plugin.setConfiguration(null));
                return plugins;
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/plugin", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<PluginMetaData> getTenantPlugins(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(pluginService.findTenantPlugins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deletePlugin(@PathVariable(PLUGIN_ID) String strPluginId) throws ThingsboardException {
        checkParameter(PLUGIN_ID, strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            PluginMetaData plugin = checkPlugin(pluginService.findPluginById(pluginId));
            pluginService.deletePluginById(pluginId);
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(pluginId, plugin,
                    null,
                    ActionType.DELETED, null, strPluginId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.PLUGIN),
                    null,
                    null,
                    ActionType.DELETED, e, strPluginId);
            throw handleException(e);
        }
    }


}
