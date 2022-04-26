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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;

@RestController
@RequestMapping("/api/entities")
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class EntitiesExportImportController extends BaseController {

    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;


    @ApiOperation(value = "Export entities by request", notes = "" +
            "Takes export request and returns list of export data for each entity found by request. " +
            "Supported entity types for export, hence for import, are **DEVICE**, **DEVICE_PROFILE**, **ASSET**, " +
            "**CUSTOMER**, **RULE_CHAIN** and **DASHBOARD**." + NEW_LINE +
            "For each type of export request, you can set some export settings: \n" +
            "- **exportRelations** - whether to export inbound and outbound relations for an entity " +
            "(only relations of type group COMMON can be exported)" + NEW_LINE +
            "Supported export requests:\n" +
            "- **SINGLE_ENTITY**:" + NEW_LINE +
            "  To export a single entity by id. Example:" + NEW_LINE +
            "```\n{\n  \"type\": \"SINGLE_ENTITY\",\n  \"entityId\": {\n    \"entityType\": \"DEVICE\",\n    \"id\": \"2eb16d70-989d-11ec-93b5-6de6c2b68078\"\n  },\n  \"exportSettings\": {\n    \"exportRelations\": false\n  }\n}\n```" + NEW_LINE +
            "- **ENTITY_LIST**:" + NEW_LINE +
            "  To export a list of entities by their ids. Example:" + NEW_LINE +
            "```\n{\n  \"type\": \"ENTITY_LIST\",\n  \"entitiesIds\": [\n    {\n      \"entityType\": \"DEVICE\",\n      \"id\": \"2eb16d70-989d-11ec-93b5-6de6c2b68078\"\n    },\n    {\n      \"entityType\": \"ASSET\",\n      \"id\": \"2f0a3bd0-989d-11ec-93b5-6de6c2b68078\"\n    }\n  ],\n  \"exportSettings\": {\n    \"exportRelations\": true\n  }\n}\n```" + NEW_LINE +
            "- **ENTITY_TYPE**:" + NEW_LINE +
            "  To export entities of specified entity type. You need to specify page size, " +
            "and may specify page index and customer id (to limit the list of entities to the ones owned by a customer). " +
            "Entities are ordered by created time descendingly. Example:" + NEW_LINE +
            "```\n{\n  \"type\": \"ENTITY_TYPE\",\n  \"entityType\": \"ASSET\",\n  \"page\": 0,\n  \"pageSize\": 100,\n  \"customerId\": \"2eb16d70-989d-11ec-93b5-6de6c2b68078\"\n}\n```" + NEW_LINE +
            "- **CUSTOM_ENTITY_FILTER**:" + NEW_LINE +
            "  To export entities by custom entity filter. The order used is the same as for ENTITY_TYPE export request. Example:" + NEW_LINE +
            "```\n{\n  \"type\": \"CUSTOM_ENTITY_FILTER\",\n  \"filter\": {\n    \"type\": \"deviceType\",\n    \"deviceType\": \"Thermostats\",\n    \"deviceNameFilter\": \"\"\n  },\n  \"page\": 0,\n  \"pageSize\": 100,\n  \"customerId\": null,\n  \"exportSettings\": {\n    \"exportRelations\": false\n  }\n}\n```" + NEW_LINE +
            "- **CUSTOM_ENTITY_QUERY**:" + NEW_LINE +
            "  To export entities by custom entity query. Example: " + NEW_LINE +
            "```\n{\n  \"type\": \"CUSTOM_ENTITY_QUERY\",\n  \"query\": {\n    \"entityFilter\": {\n      \"type\": \"entityType\",\n      \"entityType\": \"DEVICE\"\n    },\n    \"pageLink\": {\n      \"page\": 0,\n      \"pageSize\": 200,\n      \"textSearch\": \"THB_\",\n      \"sortOrder\": {\n        \"key\": {\n          \"type\": \"ENTITY_FIELD\",\n          \"key\": \"name\"\n        },\n        \"direction\": \"DESC\"\n      }\n    },\n    \"entityFields\": [\n      {\n        \"type\": \"ENTITY_FIELD\",\n        \"key\": \"name\"\n      }\n    ],\n    \"latestValues\": [\n      {\n        \"type\": \"SERVER_ATTRIBUTE\",\n        \"key\": \"lastActivityTime\"\n      }\n    ],\n    \"keyFilters\": [\n      {\n        \"key\": {\n          \"type\": \"SERVER_ATTRIBUTE\",\n          \"key\": \"lastActivityTime\"\n        },\n        \"valueType\": \"NUMERIC\",\n        \"predicate\": {\n          \"type\": \"NUMERIC\",\n          \"operation\": \"GREATER\",\n          \"value\": {\n            \"defaultValue\": 0\n          }\n        }\n      }\n    ]\n  },\n  \"customerId\": null,\n  \"exportSettings\": {\n    \"exportRelations\": false\n  }\n}\n```" + NEW_LINE +
            "Mostly, export data of an entity contains the whole entity itself and its relations " +
            "(if option to export relations was enabled):" + NEW_LINE +
            "```\n[\n  {\n    \"entityType\": \"ASSET\",\n    \"entity\": {\n      \"id\": { ... },\n      \"createdTime\": 1648204424029,\n      \"additionalInfo\": {\n        \"description\": \"\"\n      },\n      \"tenantId\": { ... },\n      \"customerId\": { ... },\n      \"name\": \"Asset 1\",\n      \"type\": \"A\",\n      ...\n    },\n    \"relations\": [\n      {\n        \"from\": {\n          \"entityType\": \"ASSET\",\n          \"id\": ...\n        },\n        \"to\": {\n          \"entityType\": \"DEVICE\",\n          \"id\": ...\n        },\n        \"type\": \"Contains\",\n        \"typeGroup\": \"COMMON\",\n        \"additionalInfo\": {\n          \"a\": \"b\"\n        }\n      }\n    ]\n  }\n]\n```" + NEW_LINE +
            "For devices, export data will additionally contain device's credentials; for rule chains - its metadata:" + NEW_LINE +
            "```\n[\n  {\n    \"entityType\": \"DEVICE\",\n    \"entity\": { ... },\n    \"credentials\": {\n      \"id\": { ... },\n      \"createdTime\": 1648829321209,\n      \"deviceId\": { ... },\n      \"credentialsType\": \"ACCESS_TOKEN\",\n      \"credentialsId\": \"5cZEDo45KGW7JgVNv4Ko\",\n      \"credentialsValue\": null\n    }\n  }\n]\n```" + NEW_LINE +
            "```\n[\n  {\n    \"entityType\": \"RULE_CHAIN\",\n    \"entity\": {\n      \"id\": { ... },\n      \"createdTime\": 1646056614257,\n      \"additionalInfo\": null,\n      \"tenantId\": { ... },\n      \"name\": \"Rule Chain 2\",\n      \"type\": \"CORE\",\n      \"firstRuleNodeId\": { ... },\n      \"root\": false,\n      ...\n    },\n    \"metaData\": {\n      \"ruleChainId\": { ... },\n      \"firstNodeIndex\": 7,\n      \"nodes\": [ ... ],\n      \"connections\": [ ... ],\n      \"ruleChainConnections\": null\n    }\n  }\n]\n```" + NEW_LINE +
            "Returned export data is to be used later for import request." + NEW_LINE +
            "If any entity found by request is of unsupported type - an error will be returned.\n" +
            "Also, if a user does not have a READ permission for an entity (or, if relations are exported, for a bounded entity), " +
            "access will be denied." +
            ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody ExportRequest exportRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            return exportEntitiesByRequest(user, exportRequest);
        } catch (Exception e) {
            log.warn("Failed to export entities for request {}", exportRequest, e);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Export entities by multiple requests", notes = "" +
            "The API behaviour is the same as for exporting entities by single request, " +
            "except that this method takes an array of export requests as a request body." + NEW_LINE +
            "Example:" + NEW_LINE +
            "```\n[\n  {\n    \"type\": \"SINGLE_ENTITY\",\n    \"entityId\": {\n      \"entityType\": \"DEVICE_PROFILE\",\n      \"id\": \"5f9eda10-b442-11ec-bbf5-adec34031568\"\n    }\n  },\n  {\n    \"type\": \"CUSTOM_ENTITY_FILTER\",\n    \"filter\": {\n      \"type\": \"deviceType\",\n      \"deviceType\": \"thermostat\",\n      \"deviceNameFilter\": \"\"\n    },\n    \"pageSize\": 1000\n  },\n  {\n    \"type\": \"ENTITY_TYPE\",\n    \"entityType\": \"ASSET\",\n    \"pageSize\": 1000,\n    \"exportSettings\": {\n      \"exportRelations\": true\n    }\n  },\n  {\n    \"type\": \"ENTITY_LIST\",\n    \"entitiesIds\": [\n      {\n        \"entityType\": \"RULE_CHAIN\",\n        \"id\": \"2ef13590-989d-11ec-93b5-6de6c2b68078\"\n      },\n      {\n        \"entityType\": \"RULE_CHAIN\",\n        \"id\": \"e7311ec0-b442-11ec-bbf5-adec34031568\"\n      }\n    ]\n  }\n]\n```" +
            ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping(value = "/export", params = {"multiple"})
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<EntityExportData<?>> exportEntities(@RequestBody List<ExportRequest> exportRequests) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            List<EntityExportData<?>> result = new ArrayList<>();
            for (ExportRequest exportRequest : exportRequests) {
                List<EntityExportData<?>> exportDataList = exportEntitiesByRequest(user, exportRequest);
                result.addAll(exportDataList);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to export entities for requests {}", exportRequests, e);
            throw handleException(e);
        }
    }

    private List<EntityExportData<?>> exportEntitiesByRequest(SecurityUser user, ExportRequest exportRequest) throws ThingsboardException {
        List<EntityId> entities = exportableEntitiesService.findEntitiesForRequest(user.getTenantId(), exportRequest);

        EntityExportSettings exportSettings = exportRequest.getExportSettings();
        if (exportSettings == null) {
            exportSettings = EntityExportSettings.builder()
                    .exportRelations(false)
                    .build();
        }

        List<EntityExportData<?>> exportDataList = new ArrayList<>();
        for (EntityId entityId : entities) {
            EntityExportData<?> exportData = exportImportService.exportEntity(user, entityId, exportSettings);
            exportDataList.add(exportData);
        }
        return exportDataList;
    }


    @ApiOperation(value = "Import entities by request", notes = "" +
            "Takes import request and returns the list of import results. " +
            "Import request must contain the list of export data and might contain import settings. " + NEW_LINE +
            "The method creates an entity if it is new in the scope of a tenant, or otherwise updates an existing one. " +
            "On entity import request, we first try to find an entity within a tenant that has externalId equal " +
            "to the id in the export data. If the platform fails to do that, we then search for an entity with " +
            "regular (internal) id like the one in the export data (this is useful in case we are exporting and " +
            "importing entities within the same tenant). Then, if we still haven't found any entity, if findExistingByName " +
            "option of the EntityImportSettings is enabled, we will search for the one by its name (this is also useful " +
            "for avoiding conflicts with default device profile or Root Rule Chain when importing all entities from another " +
            "tenant). After, if the exported entity is new for this tenant, we simply save it with external id " +
            "from the export data, and also create relations if any (if updateRelations option is enabled). " +
            "Otherwise, we will reset all fields of the existing entity to the ones from the export data and save it, " +
            "and also will update the list of relations (remove the ones that aren't present in the export data, " +
            "and update or create others), if updateRelations option is enabled." + NEW_LINE +
            "If an entity contains references to some other entities, like device references certain device profile, " +
            "we will find this other entity within the tenant by this principle: look for an entity with " +
            "such external id, or otherwise, internal id. This requires referenced entities to be imported " +
            "before the referencing entity (if we are importing to another tenant). So, when receiving the list " +
            "of entities' export data for import, we first try to fix the data order to import 'standalone' entities first." + NEW_LINE +
            "As for relations importing, they are processed after all entities in the import batch are already saved, " +
            "and the internal id of a bounded entity is found with the regular principle." + NEW_LINE +
            "Import of all entities and their relations from the import request is processed in the single transaction, " +
            "and so everything will be rolled back if the platform fails to e.g. find internal entity by external id. \n" +
            "Example of import request:\n" +
            "```\n{\n  \"importSettings\": {\n    \"findExistingByName\": false,\n    \"updateRelations\": false\n  },\n  \"exportDataList\": [\n    {\n      \"entityType\": \"DEVICE_PROFILE\",\n      \"entity\": {\n        \"id\": {\n          \"entityType\": \"DEVICE_PROFILE\",\n          \"id\": \"f84363d0-b442-11ec-bbf5-adec34031568\"\n        },\n        \"createdTime\": 1649096026765,\n        \"tenantId\": {\n          \"entityType\": \"TENANT\",\n          \"id\": \"4c9001b0-b442-11ec-bbf5-adec34031568\"\n        },\n        \"name\": \"Profile 1\",\n        ...\n      }\n    },\n    {\n      \"entityType\": \"DEVICE\",\n      \"entity\": {\n        \"id\": {\n          \"entityType\": \"DEVICE\",\n          \"id\": \"98161420-b4ca-11ec-ab0c-e7744c90d468\"\n        },\n        \"createdTime\": 1649154276962,\n        \"tenantId\": {\n          \"entityType\": \"TENANT\",\n          \"id\": \"4c9001b0-b442-11ec-bbf5-adec34031568\"\n        },\n        \"customerId\": {\n          \"entityType\": \"CUSTOMER\",\n          \"id\": \"13814000-1dd2-11b2-8080-808080808080\"\n        },\n        \"name\": \"Device 1\",\n        \"type\": \"Profile 1\",\n        \"label\": \"v1.0\",\n        \"deviceProfileId\": {\n          \"entityType\": \"DEVICE_PROFILE\",\n          \"id\": \"f84363d0-b442-11ec-bbf5-adec34031568\"\n        },\n        ...\n      },\n      \"credentials\": {\n        \"id\": {\n          \"id\": \"981e0360-b4ca-11ec-ab0c-e7744c90d468\"\n        },\n        \"createdTime\": 1649154277014,\n        \"deviceId\": {\n          \"entityType\": \"DEVICE\",\n          \"id\": \"98161420-b4ca-11ec-ab0c-e7744c90d468\"\n        },\n        \"credentialsType\": \"ACCESS_TOKEN\",\n        \"credentialsId\": \"sGExNdnl71uKmkNvtNdp\",\n        \"credentialsValue\": null\n      }\n    }\n  ]\n}\n```" + NEW_LINE +
            "The response contains a list of EntityImportResult which has values of savedEntity and oldEntity:\n" +
            "```\n[\n  {\n    \"savedEntity\": {\n      \"id\": {\n        \"entityType\": \"ASSET\",\n        \"id\": \"d73d7690-b4e6-11ec-b9eb-0562e1a20a1b\"\n      },\n      \"createdTime\": 1649166408825,\n      \"additionalInfo\": {\n        \"description\": \"\"\n      },\n      \"tenantId\": {\n        \"entityType\": \"TENANT\",\n        \"id\": \"c0b2e4f0-b4e6-11ec-b9eb-0562e1a20a1b\"\n      },\n      \"name\": \"Asset 1\",\n      \"type\": \"A\",\n      \"label\": \"v2.0\",\n      ...\n      \"externalId\": {\n        \"entityType\": \"ASSET\",\n        \"id\": \"6b03ab20-989e-11ec-b446-89df822d7fa2\"\n      }\n    },\n    \"oldEntity\": {\n      \"id\": {\n        \"entityType\": \"ASSET\",\n        \"id\": \"d73d7690-b4e6-11ec-b9eb-0562e1a20a1b\"\n      },\n      \"createdTime\": 1649166408825,\n      \"tenantId\": {\n        \"entityType\": \"TENANT\",\n        \"id\": \"c0b2e4f0-b4e6-11ec-b9eb-0562e1a20a1b\"\n      },\n      \"name\": \"Asset 1\",\n      \"type\": \"A\",\n      \"label\": \"v1.0\",\n      ...\n      \"externalId\": null\n    },\n    \"entityType\": \"ASSET\"\n  },\n  {\n    \"savedEntity\": {\n      \"id\": {\n        \"entityType\": \"ASSET\",\n        \"id\": \"387213f0-b4ea-11ec-abfc-6dcc6508d0b5\"\n      },\n      \"createdTime\": 1649167860399,\n      \"tenantId\": {\n        \"entityType\": \"TENANT\",\n        \"id\": \"c0b2e4f0-b4e6-11ec-b9eb-0562e1a20a1b\"\n      },\n      \"name\": \"Asset 2\",\n      \"type\": \"B\",\n      ...\n      \"externalId\": {\n        \"entityType\": \"ASSET\",\n        \"id\": \"0b9ea0d0-ac27-11ec-a2a6-89d15eae3b21\"\n      }\n    },\n    \"oldEntity\": null,\n    \"entityType\": \"ASSET\"\n  }\n]\n```")
    @PostMapping("/import")
    public List<EntityImportResult<?>> importEntities(@RequestBody ImportRequest importRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        try {
            EntityImportSettings importSettings = importRequest.getImportSettings();
            if (importSettings == null) {
                importSettings = EntityImportSettings.builder()
                        .findExistingByName(false)
                        .updateRelations(false)
                        .build();
            }

            List<EntityImportResult<?>> importResults = exportImportService.importEntities(user, importRequest.getExportDataList(), importSettings);

            importResults.stream()
                    .map(EntityImportResult::getSendEventsCallback)
                    .filter(Objects::nonNull)
                    .forEach(sendEventsCallback -> {
                        try {
                            sendEventsCallback.run();
                        } catch (Exception e) {
                            log.error("Failed to send event for entity", e);
                        }
                    });

            return importResults;
        } catch (Exception e) {
            log.warn("Failed to import entities for request {}", importRequest, e);
            throw handleException(e);
        }
    }

}
