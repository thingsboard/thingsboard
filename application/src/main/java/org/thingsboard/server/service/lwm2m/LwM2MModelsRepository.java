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
package org.thingsboard.server.service.lwm2m;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.LwM2mInstance;
import org.thingsboard.server.common.data.LwM2mObject;
import org.thingsboard.server.common.data.LwM2mResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.device.DeviceProfileRepository;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mGetModels;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@Component("LwM2MModelsRepository")
@ComponentScan("org.thingsboard.server.transport.lwm2m.utils")
@ConditionalOnExpression("('${(service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled}'=='true') || ('${service.type:null}'=='monolith'  && '${transport.lwm2m.enabled}'=='true') || '${service.type:null}'=='tb-core'")
public class LwM2MModelsRepository {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    LwM2mGetModels lwM2mGetModels;

    /**
     * @param objectIds
     * @param textSearch
     * @return list of LwM2mObject
     * Filter by Predicate (uses objectIds, if objectIds is null then it uses textSearch,
     * if textSearch is null then it uses AllList from  List<ObjectModel>)
     */
    public List<LwM2mObject> getLwm2mObjects(int[] objectIds, String textSearch) {
        return getLwm2mObjects((objectIds != null && objectIds.length > 0) ?
                (ObjectModel element) -> IntStream.of(objectIds).anyMatch(x -> x == element.id) :
                (textSearch != null && !textSearch.isEmpty()) ? (ObjectModel element) -> element.name.contains(textSearch) : null);
    }

    /**
     * @param predicate
     * @return list of LwM2mObject
     */
    private List<LwM2mObject> getLwm2mObjects(Predicate<? super ObjectModel> predicate) {
        List<LwM2mObject> lwM2mObjects = new ArrayList<>();
        List<ObjectModel> listObjects = (predicate == null) ? lwM2mGetModels.getModels() :
                lwM2mGetModels.getModels().stream()
                        .filter(predicate)
                        .collect(Collectors.toList());
        listObjects.forEach(obj -> {
            LwM2mObject lwM2mObject = new LwM2mObject();
            lwM2mObject.setId(obj.id);
            lwM2mObject.setName(obj.name);
            LwM2mInstance instance = new LwM2mInstance();
            instance.setId(0);
            List<LwM2mResource> resources = new ArrayList<>();
            obj.resources.forEach((k, v) -> {
                if (!v.operations.isExecutable()) {
                    LwM2mResource resource = new LwM2mResource(k, v.name, false, false, false, getCamelCase (v.name));
                    resources.add(resource);
                }
            });
            instance.setResources(resources.stream().toArray(LwM2mResource[]::new));
            lwM2mObject.setInstances(new LwM2mInstance[]{instance});
            lwM2mObjects.add(lwM2mObject);
        });
        return lwM2mObjects;
    }

    /**
     * @param tenantId
     * @param pageLink
     * @return List of LwM2mObject in PageData format
     */
    public PageData<LwM2mObject> findDeviceLwm2mObjects(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return this.findLwm2mListObjects(pageLink);
    }

    /**
     * @param pageLink
     * @return List of LwM2mObject in PageData format, filter == TextSearch
     * PageNumber = 1, PageSize = List<LwM2mObject>.size()
     */
    public PageData<LwM2mObject> findLwm2mListObjects(PageLink pageLink) {
        PageImpl page = new PageImpl(getLwm2mObjects(null, pageLink.getTextSearch()));
        PageData pageData = new PageData(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
        return pageData;
    }

    private String getCamelCase (String name) {
        return name;
    }
}

