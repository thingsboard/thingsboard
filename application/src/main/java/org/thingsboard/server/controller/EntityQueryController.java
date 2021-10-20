/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.query.EntityQueryService;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityQueryController extends BaseController {

    private static final String SINGLE_ENTITY = "\n\n## Single Entity\n\n" +
            "Allows to filter only one entity based on the id. For example, this entity filter selects certain device:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"singleEntity\",\n" +
            "  \"singleEntity\": {\n" +
            "    \"id\": \"d521edb0-2a7a-11ec-94eb-213c95f54092\",\n" +
            "    \"entityType\": \"DEVICE\"\n" +
            "  }\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String ENTITY_LIST = "\n\n## Entity List Filter\n\n" +
            "Allows to filter entities of the same type using their ids. For example, this entity filter selects two devices:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityList\",\n" +
            "  \"entityType\": \"DEVICE\",\n" +
            "  \"entityList\": [\n" +
            "    \"e6501f30-2a7a-11ec-94eb-213c95f54092\",\n" +
            "    \"e6657bf0-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String ENTITY_NAME = "\n\n## Entity Name Filter\n\n" +
            "Allows to filter entities of the same type using the **'starts with'** expression over entity name. " +
            "For example, this entity filter selects all devices which name starts with 'Air Quality':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityName\",\n" +
            "  \"entityType\": \"DEVICE\",\n" +
            "  \"entityNameFilter\": \"Air Quality\"\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String ENTITY_TYPE = "\n\n## Entity Type Filter\n\n" +
            "Allows to filter entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc)" +
            "For example, this entity filter selects all tenant customers:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityType\",\n" +
            "  \"entityType\": \"CUSTOMER\"\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String ASSET_TYPE = "\n\n## Asset Type Filter\n\n" +
            "Allows to filter assets based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'charging station' assets which name starts with 'Tesla':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"assetType\",\n" +
            "  \"assetType\": \"charging station\",\n" +
            "  \"assetNameFilter\": \"Tesla\"\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String DEVICE_TYPE = "\n\n## Device Type Filter\n\n" +
            "Allows to filter devices based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'Temperature Sensor' devices which name starts with 'ABC':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"deviceType\",\n" +
            "  \"deviceType\": \"Temperature Sensor\",\n" +
            "  \"deviceNameFilter\": \"ABC\"\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String EDGE_TYPE = "\n\n## Edge Type Filter\n\n" +
            "Allows to filter edge instances based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'Factory' edge instances which name starts with 'Nevada':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"edgeType\",\n" +
            "  \"edgeType\": \"Factory\",\n" +
            "  \"edgeNameFilter\": \"Nevada\"\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String ENTITY_VIEW_TYPE = "\n\n## Entity View Filter\n\n" +
            "Allows to filter entity views based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'Concrete Mixer' entity views which name starts with 'CAT':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityViewType\",\n" +
            "  \"entityViewType\": \"Concrete Mixer\",\n" +
            "  \"entityViewNameFilter\": \"CAT\"\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String API_USAGE = "\n\n## Api Usage Filter\n\n" +
            "Allows to query for Api Usage based on optional customer id. If the customer id is not set, returns current tenant API usage." +
            "For example, this entity filter selects the 'Api Usage' entity for customer with id 'e6501f30-2a7a-11ec-94eb-213c95f54092':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"apiUsageState\",\n" +
            "  \"customerId\": {\n" +
            "    \"id\": \"d521edb0-2a7a-11ec-94eb-213c95f54092\",\n" +
            "    \"entityType\": \"CUSTOMER\"\n" +
            "  }\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String MAX_LEVEL_DESCRIPTION = "Possible direction values are 'TO' and 'FROM'. The 'maxLevel' defines how many relation levels should the query search 'recursively'. ";
    private static final String FETCH_LAST_LEVEL_ONLY_DESCRIPTION = "Assuming the 'maxLevel' is > 1, the 'fetchLastLevelOnly' defines either to return all related entities or only entities that are on the last level of relations. ";

    private static final String RELATIONS_QUERY_FILTER = "\n\n## Relations Query Filter\n\n" +
            "Allows to filter entities that are related to the provided root entity. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'filter' object allows you to define the relation type and set of acceptable entity types to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only those who match the 'filters'.\n\n" +
            "For example, this entity filter selects all devices and assets which are related to the asset with id 'e51de0c0-2a7a-11ec-94eb-213c95f54092':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"relationsQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e51de0c0-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 1,\n" +
            "  \"fetchLastLevelOnly\": false,\n" +
            "  \"filters\": [\n" +
            "    {\n" +
            "      \"relationType\": \"Contains\",\n" +
            "      \"entityTypes\": [\n" +
            "        \"DEVICE\",\n" +
            "        \"ASSET\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";


    private static final String ASSET_QUERY_FILTER = "\n\n## Asset Search Query\n\n" +
            "Allows to filter assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'assetTypes' defines the type of the asset to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only assets that match 'relationType' and 'assetTypes' conditions.\n\n" +
            "For example, this entity filter selects 'charging station' assets which are related to the asset with id 'e51de0c0-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"assetSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e51de0c0-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 1,\n" +
            "  \"fetchLastLevelOnly\": false,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"assetTypes\": [\n" +
            "    \"charging station\"\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String DEVICE_QUERY_FILTER = "\n\n## Device Search Query\n\n" +
            "Allows to filter devices that are related to the provided root entity. Filters related devices based on the relation type and set of device types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'deviceTypes' defines the type of the device to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only devices that match 'relationType' and 'deviceTypes' conditions.\n\n" +
            "For example, this entity filter selects 'Charging port' and 'Air Quality Sensor' devices which are related to the asset with id 'e52b0020-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"deviceSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e52b0020-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 2,\n" +
            "  \"fetchLastLevelOnly\": true,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"deviceTypes\": [\n" +
            "    \"Air Quality Sensor\",\n" +
            "    \"Charging port\"\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String EV_QUERY_FILTER = "\n\n## Entity View Query\n\n" +
            "Allows to filter entity views that are related to the provided root entity. Filters related entity views based on the relation type and set of entity view types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'entityViewTypes' defines the type of the entity view to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only devices that match 'relationType' and 'deviceTypes' conditions.\n\n" +
            "For example, this entity filter selects 'Concrete mixer' entity views which are related to the asset with id 'e52b0020-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityViewSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e52b0020-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 1,\n" +
            "  \"fetchLastLevelOnly\": false,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"entityViewTypes\": [\n" +
            "    \"Concrete mixer\"\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String EDGE_QUERY_FILTER = "\n\n## Edge Search Query\n\n" +
            "Allows to filter edge instances that are related to the provided root entity. Filters related edge instances based on the relation type and set of edge types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'deviceTypes' defines the type of the device to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only devices that match 'relationType' and 'deviceTypes' conditions.\n\n" +
            "For example, this entity filter selects 'Factory' edge instances which are related to the asset with id 'e52b0020-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"deviceSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e52b0020-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 2,\n" +
            "  \"fetchLastLevelOnly\": true,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"edgeTypes\": [\n" +
            "    \"Factory\"\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String EMPTY = "\n\n## Entity Type Filter\n\n" +
            "Allows to filter multiple entities of the same type using the **'starts with'** expression over entity name. " +
            "For example, this entity filter selects all devices which name starts with 'Air Quality':\n\n"+
            MARKDOWN_CODE_BLOCK_START +
            ""+
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String ENTITY_FILTERS =
            "\n\n # Entity Filters" +
            "\nEntity Filter body depends on the 'type' parameter. Let's review available entity filter types. In fact, they do correspond to available dashboard aliases." +
            SINGLE_ENTITY + ENTITY_LIST + ENTITY_NAME + ENTITY_TYPE + ASSET_TYPE + DEVICE_TYPE + EDGE_TYPE + ENTITY_VIEW_TYPE + API_USAGE + RELATIONS_QUERY_FILTER
            + ASSET_QUERY_FILTER + DEVICE_QUERY_FILTER + EV_QUERY_FILTER + EDGE_QUERY_FILTER;

    private static final String FILTER_KEY = "\n\n## Filter Key\n\n" +
            "Filter Key defines either entity field, attribute or telemetry. It is a JSON object that consists the key name and type. " +
            "The following filter key types are supported: \n\n"+
            " * 'CLIENT_ATTRIBUTE' - used for client attributes; \n" +
            " * 'SHARED_ATTRIBUTE' - used for shared attributes; \n" +
            " * 'SERVER_ATTRIBUTE' - used for server attributes; \n" +
            " * 'ATTRIBUTE' - used for any of the above; \n" +
            " * 'TIME_SERIES' - used for time-series values; \n" +
            " * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type; \n" +
            " * 'ALARM_FIELD' - similar to entity field, but is used in alarm queries only; \n" +
            "\n\n Let's review the example:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"TIME_SERIES\",\n" +
            "  \"key\": \"temperature\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    private static final String FILTER_VALUE_TYPE = "\n\n## Value Type and Operations\n\n" +
            "Provides a hint about the data type of the entity field that is defined in the filter key. " +
            "The value type impacts the list of possible operations that you may use in the corresponding predicate. For example, you may use 'STARTS_WITH' or 'END_WITH', but you can't use 'GREATER_OR_EQUAL' for string values." +
            "The following filter value types and corresponding predicate operations are supported: \n\n"+
            " * 'STRING' - used to filter any 'String' or 'JSON' values. Operations: EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS; \n" +
            " * 'NUMERIC' - used for 'Long' and 'Double' values. Operations: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL; \n" +
            " * 'BOOLEAN' - used for boolean values; Operations: EQUAL, NOT_EQUAL \n" +
            " * 'DATE_TIME' - similar to numeric, transforms value to milliseconds since epoch. Operations: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL; \n";

    private static final String FILTER_PREDICATE = "\n\n## Filter Predicate\n\n" +
            "Filter Predicate defines the logical expression to evaluate. The list of available operations depends on the filter value type, see above. " +
            "Platform supports 4 predicate types: 'STRING', 'NUMERIC', 'BOOLEAN' and 'COMPLEX'. The last one allows to combine multiple operations over one filter key." +
            "\n\nSimple predicate example to check 'value < 100': \n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"LESS\",\n" +
            "  \"value\": {\n" +
            "    \"defaultValue\": 100,\n" +
            "    \"dynamicValue\": null\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\nComplex predicate example, to check 'value < 10 or value > 20': \n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"operation\": \"GREATER\",\n" +
            "      \"value\": {\n" +
            "        \"defaultValue\": 20,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\nMore complex predicate example, to check 'value < 10 or (value > 50 && value < 60)': \n\n"+
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"COMPLEX\",\n" +
            "      \"operation\": \"AND\",\n" +
            "      \"predicates\": [\n" +
            "        {\n" +
            "          \"operation\": \"GREATER\",\n" +
            "          \"value\": {\n" +
            "            \"defaultValue\": 50,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"operation\": \"LESS\",\n" +
            "          \"value\": {\n" +
            "            \"defaultValue\": 60,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n You may also want to replace hardcoded values (for example, temperature > 20) with the more dynamic " +
            "expression (for example, temperature > 'value of the tenant attribute with key 'temperatureThreshold'). " +
            "It is possible to use 'dynamicValue' to define attribute of the tenant, customer or user that is performing the API call. " +
            "See example below: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"GREATER\",\n" +
            "  \"value\": {\n" +
            "    \"defaultValue\": 0,\n" +
            "    \"dynamicValue\": {\n" +
            "      \"sourceType\": \"CURRENT_USER\",\n" +
            "      \"sourceAttribute\": \"temperatureThreshold\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n Note that you may use 'CURRENT_USER', 'CURRENT_CUSTOMER' and 'CURRENT_TENANT' as a 'sourceType'. The 'defaultValue' is used when the attribute with such a name is not defined for the chosen source.";

    private static final String KEY_FILTERS =
            "\n\n # Key Filters" +
                    "\nKey Filter allows you to define complex logical expressions over entity field, attribute or latest time-series value. The filter is defined using 'key', 'valueType' and 'predicate' objects. " +
                    "Single Entity Query may have zero, one or multiple predicates. If multiple filters are defined, they are evaluated using logical 'AND'. " +
                    "The example below checks that temperature of the entity is above 20 degrees:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"key\": {\n" +
                    "    \"type\": \"TIME_SERIES\",\n" +
                    "    \"key\": \"temperature\"\n" +
                    "  },\n" +
                    "  \"valueType\": \"NUMERIC\",\n" +
                    "  \"predicate\": {\n" +
                    "    \"operation\": \"GREATER\",\n" +
                    "    \"value\": {\n" +
                    "      \"defaultValue\": 20,\n" +
                    "      \"dynamicValue\": null\n" +
                    "    },\n" +
                    "    \"type\": \"NUMERIC\"\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Now let's review 'key', 'valueType' and 'predicate' objects in detail."
                    + FILTER_KEY + FILTER_VALUE_TYPE + FILTER_PREDICATE;

    private static final String ENTITY_COUNT_QUERY_DESCRIPTION =
            "Allows to run complex queries to search the count of platform entities (devices, assets, customers, etc) " +
                "based on the combination of main entity filter and multiple key filters. Returns the number of entities that match the query definition.\n\n" +
            "# Query Definition\n\n" +
                "\n\nMain **entity filter** is mandatory and defines generic search criteria. " +
                    "For example, \"find all devices with profile 'Moisture Sensor'\" or \"Find all devices related to asset 'Building A'\"" +
                "\n\nOptional **key filters** allow to filter results of the entity filter by complex criteria against " +
                        "main entity fields (name, label, type, etc), attributes and telemetry. " +
                        "For example, \"temperature > 20 or temperature< 10\" or \"name starts with 'T', and attribute 'model' is 'T1000', and timeseries field 'batteryLevel' > 40\"."+
                "\n\nLet's review the example:" +
                "\n\n" + MARKDOWN_CODE_BLOCK_START +
                "{\n" +
                    "  \"entityFilter\": {\n" +
                    "    \"type\": \"entityType\",\n" +
                    "    \"entityType\": \"DEVICE\"\n" +
                    "  },\n" +
                    "  \"keyFilters\": [\n" +
                    "    {\n" +
                    "      \"key\": {\n" +
                    "        \"type\": \"ATTRIBUTE\",\n" +
                    "        \"key\": \"active\"\n" +
                    "      },\n" +
                    "      \"valueType\": \"BOOLEAN\",\n" +
                    "      \"predicate\": {\n" +
                    "        \"operation\": \"EQUAL\",\n" +
                    "        \"value\": {\n" +
                    "          \"defaultValue\": true,\n" +
                    "          \"dynamicValue\": null\n" +
                    "        },\n" +
                    "        \"type\": \"BOOLEAN\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}"+
                MARKDOWN_CODE_BLOCK_END +
                "\n\n Example mentioned above search all devices which have attribute 'active' set to 'true'. Now let's review available entity filters and key filters syntax:" +
                ENTITY_FILTERS +
                KEY_FILTERS +
                TENANT_AND_USER_AUTHORITY_PARAGRAPH;;

    private static final String ENTITY_DATA_QUERY_DESCRIPTION =
            "Allows to run complex queries over platform entities (devices, assets, customers, etc) " +
                    "based on the combination of main entity filter and multiple key filters. " +
                    "Returns the paginated result of the query that contains requested entity fields and latest values of requested attributes and time-series data.\n\n" +
                    "# Query Definition\n\n" +
                    "\n\nMain **entity filter** is mandatory and defines generic search criteria. " +
                    "For example, \"find all devices with profile 'Moisture Sensor'\" or \"Find all devices related to asset 'Building A'\"" +
                    "\n\nOptional **key filters** allow to filter results of the **entity filter** by complex criteria against " +
                    "main entity fields (name, label, type, etc), attributes and telemetry. " +
                    "For example, \"temperature > 20 or temperature< 10\" or \"name starts with 'T', and attribute 'model' is 'T1000', and timeseries field 'batteryLevel' > 40\"."+
                    "\n\nThe **entity fields** and **latest values** contains list of entity fields and latest attribute/telemetry fields to fetch for each entity." +
                    "\n\nThe **page link** contains information about the page to fetch and the sort ordering." +
                    "\n\nLet's review the example:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"entityFilter\": {\n" +
                    "    \"type\": \"entityType\",\n" +
                    "    \"resolveMultiple\": true,\n" +
                    "    \"entityType\": \"DEVICE\"\n" +
                    "  },\n" +
                    "  \"keyFilters\": [\n" +
                    "    {\n" +
                    "      \"key\": {\n" +
                    "        \"type\": \"TIME_SERIES\",\n" +
                    "        \"key\": \"temperature\"\n" +
                    "      },\n" +
                    "      \"valueType\": \"NUMERIC\",\n" +
                    "      \"predicate\": {\n" +
                    "        \"operation\": \"GREATER\",\n" +
                    "        \"value\": {\n" +
                    "          \"defaultValue\": 0,\n" +
                    "          \"dynamicValue\": {\n" +
                    "            \"sourceType\": \"CURRENT_USER\",\n" +
                    "            \"sourceAttribute\": \"temperatureThreshold\",\n" +
                    "            \"inherit\": false\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"type\": \"NUMERIC\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"entityFields\": [\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"name\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"label\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"additionalInfo\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"latestValues\": [\n" +
                    "    {\n" +
                    "      \"type\": \"ATTRIBUTE\",\n" +
                    "      \"key\": \"model\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"TIME_SERIES\",\n" +
                    "      \"key\": \"temperature\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"pageLink\": {\n" +
                    "    \"page\": 0,\n" +
                    "    \"pageSize\": 10,\n" +
                    "    \"sortOrder\": {\n" +
                    "      \"key\": {\n" +
                    "        \"key\": \"name\",\n" +
                    "        \"type\": \"ENTITY_FIELD\"\n" +
                    "      },\n" +
                    "      \"direction\": \"ASC\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}"+
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Example mentioned above search all devices which have attribute 'active' set to 'true'. Now let's review available entity filters and key filters syntax:" +
                    ENTITY_FILTERS +
                    KEY_FILTERS +
                    TENANT_AND_USER_AUTHORITY_PARAGRAPH;


    private static final String ALARM_DATA_QUERY_DESCRIPTION = "This method description defines how Alarm Data Query extends the Entity Data Query. " +
            "See method 'Find Entity Data by Query' first to get the info about 'Entity Data Query'." +
            "\n\n The platform will first search the entities that match the entity and key filters. Then, the platform will use 'Alarm Page Link' to filter the alarms related to those entities. " +
            "Finally, platform fetch the properties of alarm that are defined in the **'alarmFields'** and combine them with the other entity, attribute and latest time-series fields to return the result. " +
            "\n\n See example of the alarm query below. The query will search first 100 active alarms with type 'Temperature Alarm' or 'Fire Alarm' for any device with current temperature > 0. " +
            "The query will return combination of the entity fields: name of the device, device model and latest temperature reading and alarms fields: createdTime, type, severity and status: " +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"entityFilter\": {\n" +
            "    \"type\": \"entityType\",\n" +
            "    \"resolveMultiple\": true,\n" +
            "    \"entityType\": \"DEVICE\"\n" +
            "  },\n" +
            "  \"pageLink\": {\n" +
            "    \"page\": 0,\n" +
            "    \"pageSize\": 100,\n" +
            "    \"textSearch\": null,\n" +
            "    \"searchPropagatedAlarms\": false,\n" +
            "    \"statusList\": [\n" +
            "      \"ACTIVE\"\n" +
            "    ],\n" +
            "    \"severityList\": [\n" +
            "      \"CRITICAL\",\n" +
            "      \"MAJOR\"\n" +
            "    ],\n" +
            "    \"typeList\": [\n" +
            "      \"Temperature Alarm\",\n" +
            "      \"Fire Alarm\"\n" +
            "    ],\n" +
            "    \"sortOrder\": {\n" +
            "      \"key\": {\n" +
            "        \"key\": \"createdTime\",\n" +
            "        \"type\": \"ALARM_FIELD\"\n" +
            "      },\n" +
            "      \"direction\": \"DESC\"\n" +
            "    },\n" +
            "    \"timeWindow\": 86400000\n" +
            "  },\n" +
            "  \"keyFilters\": [\n" +
            "    {\n" +
            "      \"key\": {\n" +
            "        \"type\": \"TIME_SERIES\",\n" +
            "        \"key\": \"temperature\"\n" +
            "      },\n" +
            "      \"valueType\": \"NUMERIC\",\n" +
            "      \"predicate\": {\n" +
            "        \"operation\": \"GREATER\",\n" +
            "        \"value\": {\n" +
            "          \"defaultValue\": 0,\n" +
            "          \"dynamicValue\": null\n" +
            "        },\n" +
            "        \"type\": \"NUMERIC\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"alarmFields\": [\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"createdTime\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"type\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"severity\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"status\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"entityFields\": [\n" +
            "    {\n" +
            "      \"type\": \"ENTITY_FIELD\",\n" +
            "      \"key\": \"name\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"latestValues\": [\n" +
            "    {\n" +
            "      \"type\": \"ATTRIBUTE\",\n" +
            "      \"key\": \"model\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"TIME_SERIES\",\n" +
            "      \"key\": \"temperature\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"+
            MARKDOWN_CODE_BLOCK_END +
            "";

    @Autowired
    private EntityQueryService entityQueryService;

    private static final int MAX_PAGE_SIZE = 100;

    @ApiOperation(value = "Count Entities by Query", notes = ENTITY_COUNT_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countEntitiesByQuery(
            @ApiParam(value = "A JSON value representing the entity count query. See API call notes above for more details.")
            @RequestBody EntityCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Entity Data by Query", notes = ENTITY_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EntityData> findEntityDataByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Alarms by Query", notes = ALARM_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<AlarmData> findAlarmDataByQuery(
            @ApiParam(value = "A JSON value representing the alarm data query. See API call notes above for more details.")
            @RequestBody AlarmDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Entity Keys by Query",
            notes = "Uses entity data query (see 'Find Entity Data by Query') to find first 100 entities. Then fetch and return all unique time-series and/or attribute keys. Used mostly for UI hints.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> findEntityTimeseriesAndAttributesKeysByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query,
            @ApiParam(value = "Include all unique time-series keys to the result.")
            @RequestParam("timeseries") boolean isTimeseries,
            @ApiParam(value = "Include all unique attribute keys to the result.")
            @RequestParam("attributes") boolean isAttributes) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        checkNotNull(query);
        try {
            EntityDataPageLink pageLink = query.getPageLink();
            if (pageLink.getPageSize() > MAX_PAGE_SIZE) {
                pageLink.setPageSize(MAX_PAGE_SIZE);
            }
            return entityQueryService.getKeysByQuery(getCurrentUser(), tenantId, query, isTimeseries, isAttributes);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
