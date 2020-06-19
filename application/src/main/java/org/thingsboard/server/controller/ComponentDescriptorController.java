/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class ComponentDescriptorController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/component/{componentDescriptorClazz:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ComponentDescriptor getComponentDescriptorByClazz(@PathVariable("componentDescriptorClazz") String strComponentDescriptorClazz) throws ThingsboardException {
        checkParameter("strComponentDescriptorClazz", strComponentDescriptorClazz);
        try {
            return checkComponentDescriptorByClazz(strComponentDescriptorClazz);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components/{componentType}/{ruleChainType}", method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByType(@PathVariable(value = "ruleChainType", required = false) String strRuleChainType,
                                                                   @PathVariable("componentType") String strComponentType) throws ThingsboardException {
        checkParameter("componentType", strComponentType);
        try {
            return checkComponentDescriptorsByType(ComponentType.valueOf(strComponentType), getRuleChainType(strRuleChainType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components/{ruleChainType}", params = {"componentTypes"}, method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(@PathVariable(value = "ruleChainType", required = false) String strRuleChainType,
                                                                    @RequestParam("componentTypes") String[] strComponentTypes) throws ThingsboardException {
        checkArrayParameter("componentTypes", strComponentTypes);
        try {
            Set<ComponentType> componentTypes = new HashSet<>();
            for (String strComponentType : strComponentTypes) {
                componentTypes.add(ComponentType.valueOf(strComponentType));
            }
            return checkComponentDescriptorsByTypes(componentTypes, getRuleChainType(strRuleChainType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private RuleChainType getRuleChainType(String strRuleChainType) {
        RuleChainType ruleChainType;
        if (StringUtils.isEmpty(strRuleChainType)) {
            ruleChainType = RuleChainType.CORE;
        } else {
            ruleChainType = RuleChainType.valueOf(strRuleChainType);
        }
        return ruleChainType;
    }

}
