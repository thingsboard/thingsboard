package com.iot.thingmodel.common;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class Constants {

    private Constants() {
    }

    public static final class Api {

        public static final String API_PATTERN = "/api/**";
        public static final String ERROR_PATH = "/error";
        public static final String PRODUCTS_PATH = "/api/products";
        public static final String ADMIN_PATH = "/api/admin";
        public static final String AUTH_PATH = "/api/auth";
        public static final String PRODUCT_SYNC_RETRY_PATH = "/api/products/%s/sync";
        public static final String CORS_ORIGIN = "http://localhost:4200";
        public static final List<String> CORS_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        public static final String CORS_ALL_HEADERS = "*";
        public static final String OPENAPI_TITLE = "Thing Model Service";
        public static final String OPENAPI_VERSION = "0.1.0";
        public static final String OPENAPI_DESCRIPTION = "Product and thing model service for ThingsBoard";
        public static final String OPENAPI_SECURITY_SCHEME = "tbJwt";
        public static final String OPENAPI_BEARER_SCHEME = "bearer";
        public static final String OPENAPI_BEARER_FORMAT = "JWT";
    }

    public static final class Auth {

        public static final String HEADER_X_AUTHORIZATION = "X-Authorization";
        public static final String HEADER_AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
        public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
        public static final String ROLE_SYS_ADMIN = "SYS_ADMIN";
        public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
        public static final String CURRENT_USER_ATTRIBUTE = "currentUser";
        public static final int MAX_CACHE_SIZE = 10000;
        public static final long MILLIS_PER_SECOND = 1000L;
        public static final String SHA_256 = "SHA-256";
        public static final String AES = "AES";
        public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
        public static final int IV_LENGTH = 12;
        public static final int TAG_LENGTH_BITS = 128;
        public static final int AES_KEY_LENGTH_128 = 16;
        public static final int AES_KEY_LENGTH_192 = 24;
        public static final int AES_KEY_LENGTH_256 = 32;
        public static final String MASKED_PASSWORD = "***";
    }

    public static final class Product {

        public static final String STATUS_DRAFT = "DRAFT";
        public static final String STATUS_PUBLISHED = "PUBLISHED";
        public static final String STATUS_DEPRECATED = "DEPRECATED";
        public static final String AUTH_TYPE_ACCESS_TOKEN = "ACCESS_TOKEN";
        public static final String PROTOCOL_MQTT = "MQTT";
        public static final String SYNC_RESULT_OK = "OK";
        public static final String DEFAULT_TSL_VERSION = "1.0";
        public static final String DEFAULT_PROPERTY_MODE = "r";
        public static final String ASSET_TYPE_PRODUCT = "product";
        public static final String DEFAULT_PROFILE_BASE_NAME = "product";
        public static final String ASSET_NAME_PREFIX = "产品 ";
        public static final String PROFILE_NAME_SEPARATOR = " ";
        public static final String PROFILE_DESCRIPTION_FORMAT =
                "ProductId=%s; CategoryId=%s; TSL v%s; 属性 %d 个; 事件 %d 个; 行为 %d 个";
        public static final String ATTRIBUTE_PRODUCT_ID = "product_id";
        public static final String ATTRIBUTE_CATEGORY_ID = "category_id";
        public static final String ATTRIBUTE_TSL_VERSION = "tsl_version";
        public static final String ATTRIBUTE_PROPERTY_COUNT = "property_count";
        public static final String ATTRIBUTE_EVENT_COUNT = "event_count";
        public static final String ATTRIBUTE_ACTION_COUNT = "action_count";
        public static final String ATTRIBUTE_TSL_JSON = "tsl_json";
        public static final String ATTRIBUTE_TSL_SCHEMA = "tsl_schema";
        public static final String ATTRIBUTE_STRICT_UNKNOWN = "strict_unknown";
        public static final String ATTRIBUTE_TENANT_ID = "tenant_id";
        public static final String ATTRIBUTE_DEVICE_UUID = "device_uuid";
        public static final String ATTRIBUTE_SITE_CODE = "site_code";
        public static final String ENTITY_TYPE_ASSET = "ASSET";
        public static final String ATTRIBUTE_SCOPE_SERVER = "SERVER_SCOPE";
        public static final String COLUMN_TS = "ts";
        public static final String COLUMN_TS_V = "ts_v";
    }

    public static final class Tsl {

        public static final String SCHEMA_VERSION = "1.0";
        public static final String SCHEMA_RESOURCE = "/schemas/tsl-v1.5.schema.json";
        public static final String TYPE_BOOL = "bool";
        public static final String TYPE_INT = "int";
        public static final String TYPE_FLOAT = "float";
        public static final String TYPE_STRING = "string";
        public static final String TYPE_ENUM = "enum";
        public static final String TYPE_ENUM_STRING = "enum_string";
        public static final String TYPE_TIMESTAMP = "timestamp";
        public static final String TYPE_STRUCT = "struct";
        public static final String TYPE_UNKNOWN = "unknown";
        public static final String MODE_READ = "r";
        public static final String MODE_READ_WRITE = "rw";
        public static final String MODE_DEFAULT = MODE_READ;
        public static final String EVENT_TYPE_INFO = "info";
        public static final String EVENT_TYPE_ALERT = "alert";
        public static final String EVENT_TYPE_FAULT = "fault";
        public static final String KEY_TS = "ts";
        public static final String KEY_EVENT_ID = "event_id";
        public static final String KEY_EVENT_TYPE = "event_type";
        public static final String KEY_EVENT_TS = "event_ts";
        public static final String KEY_PROFILE = "profile";
        public static final String KEY_PROFILE_PRODUCT_ID = "ProductId";
        public static final String KEY_PROFILE_CATEGORY_ID = "CategoryId";
        public static final String KEY_PRODUCT_ID = "productId";
        public static final String KEY_CATEGORY_ID = "categoryId";
        public static final String KEY_VERSION = "version";
        public static final String KEY_PROPERTIES = "properties";
        public static final String KEY_EVENTS = "events";
        public static final String KEY_ACTIONS = "actions";
        public static final String KEY_DEFINE = "define";
        public static final String KEY_MAPPING = "mapping";
        public static final String KEY_ID = "id";
        public static final String KEY_NAME = "name";
        public static final String KEY_DESC = "desc";
        public static final String KEY_MODE = "mode";
        public static final String KEY_REQUIRED = "required";
        public static final String KEY_TYPE = "type";
        public static final String KEY_EVENT_TYPE_FIELD = "eventType";
        public static final String KEY_PARAMS = "params";
        public static final String KEY_OUTPUT = "output";
        public static final String KEY_INPUT = "input";
        public static final String KEY_INPUT_PARAMS = "inputParams";
        public static final String KEY_OUTPUT_PARAMS = "outputParams";
        public static final String KEY_MIN = "min";
        public static final String KEY_MAX = "max";
        public static final String KEY_START = "start";
        public static final String KEY_STEP = "step";
        public static final String KEY_UNIT = "unit";
        public static final String KEY_SPECS = "specs";
        public static final String KEY_FIELDS = "fields";
        public static final String KEY_KEYS = "keys";
        public static final String KEY_SCHEMA_VERSION = "schemaVersion";
        public static final String KEY_STRICT_UNKNOWN = "strictUnknown";
        public static final String KEY_REQUEST_TSL = "tsl";
        public static final String KEY_COMPACT_TYPE = "t";
        public static final String KEY_COMPACT_MAP = "map";
        public static final String KEY_COMPACT_REQUIRED = "req";
        public static final String KEY_COMPACT_KEY = "key";
        public static final String PATH_ROOT = "/";
        public static final String PATH_PROFILE = "/profile";
        public static final String PATH_VERSION = "/version";
        public static final String PATH_PROFILE_PRODUCT_ID = "/profile/ProductId";
        public static final String PATH_ID = "/id";
        public static final String PATH_NAME = "/name";
        public static final String PATH_MODE = "/mode";
        public static final String PATH_TYPE = "/type";
        public static final String PATH_DEFINE = "/define";
        public static final String PATH_START = "/start";
        public static final String PATH_STEP = "/step";
        public static final String PATH_MAPPING = "/mapping";
        public static final String PATH_SPECS = "/specs";
        public static final String PATH_PARAMS = "/params";
        public static final String PATH_INPUT = "/input";
        public static final String PATH_OUTPUT = "/output";
        public static final String PATH_PROPERTIES = "/properties/";
        public static final String PATH_PROPERTIES_ROOT = "/properties";
        public static final String PATH_EVENTS = "/events/";
        public static final String PATH_ACTIONS = "/actions/";
        public static final Set<String> TYPES = Set.of(
                TYPE_BOOL, TYPE_INT, TYPE_FLOAT, TYPE_STRING,
                TYPE_ENUM, TYPE_ENUM_STRING, TYPE_TIMESTAMP, TYPE_STRUCT);
        public static final Set<String> MODES = Set.of(MODE_READ, MODE_READ_WRITE);
        public static final Set<String> EVENT_TYPES = Set.of(
                EVENT_TYPE_INFO, EVENT_TYPE_ALERT, EVENT_TYPE_FAULT);
        public static final Set<String> RESERVED_KEYS = Set.of(
                KEY_TS, KEY_EVENT_ID, KEY_EVENT_TYPE, KEY_EVENT_TS);
        public static final Set<String> NUMERIC_TYPES = Set.of(
                TYPE_INT, TYPE_FLOAT, TYPE_TIMESTAMP, TYPE_ENUM);
        public static final String SCHEMA_KEYWORD_REQUIRED = "required";
        public static final String SCHEMA_KEYWORD_TYPE = "type";
        public static final String SCHEMA_KEYWORD_ENUM = "enum";
        public static final String SCHEMA_KEYWORD_PATTERN = "pattern";
        public static final String SCHEMA_KEYWORD_MAX_LENGTH = "maxLength";
        public static final String SCHEMA_KEYWORD_MINIMUM = "minimum";
        public static final String SCHEMA_KEYWORD_MAXIMUM = "maximum";
        public static final String SCHEMA_KEYWORD_EXCLUSIVE_MINIMUM = "exclusiveMinimum";
        public static final String SCHEMA_KEYWORD_EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
        public static final String SCHEMA_KEYWORD_ADDITIONAL_PROPERTIES = "additionalProperties";
    }

    public static final class Validation {

        public static final int IDENTIFIER_MAX_LENGTH = 50;
        public static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
                "^[A-Za-z_][A-Za-z0-9_]{0," + (IDENTIFIER_MAX_LENGTH - 1) + "}$");
        public static final int MAX_FUNCTION_POINTS = 100;
        public static final int MIN_ENUM_MAPPING_SIZE = 2;
        public static final int TENANT_ID_MAX_LENGTH = 64;
        public static final int PRODUCT_ID_MAX_LENGTH = 64;
        public static final int PRODUCT_NAME_MAX_LENGTH = 128;
        public static final int MODEL_MAX_LENGTH = 64;
        public static final int CATEGORY_ID_MAX_LENGTH = 64;
        public static final int TSL_VERSION_MAX_LENGTH = 32;
        public static final int USERNAME_MAX_LENGTH = 255;
        public static final String ISSUE_KEY_SEPARATOR = "|";
    }

    public static final class ThingsBoard {

        public static final int PAGE_SIZE = 100;
        public static final int MAX_PAGES = 10;
        public static final int FIRST_PAGE = 0;
        public static final String SORT_PROPERTY_NAME = "name";
        public static final String SORT_ORDER_ASC = "ASC";
        public static final String RULE_CHAIN_TYPE_CORE = "CORE";
        public static final int AUTH_RETRY_ATTEMPTS = 2;
        public static final int FIRST_ATTEMPT = 0;
        public static final int ERROR_BODY_MAX_LENGTH = 500;
    }

    public static final class RuleChain {

        public static final String MSG_TYPE_SWITCH =
                "org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode";
        public static final String GET_ATTRIBUTES =
                "org.thingsboard.rule.engine.metadata.TbGetAttributesNode";
        public static final String TRANSFORM_SCRIPT =
                "org.thingsboard.rule.engine.transform.TbTransformMsgNode";
        public static final String JS_SWITCH =
                "org.thingsboard.rule.engine.filter.TbJsSwitchNode";
        public static final String KAFKA =
                "org.thingsboard.rule.engine.kafka.TbKafkaNode";
        public static final String SAVE_TIMESERIES =
                "org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode";
        public static final String SAVE_CLIENT_ATTRIBUTES =
                "org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode";
        public static final String LOG =
                "org.thingsboard.rule.engine.action.TbLogNode";
        public static final String SEND_RPC =
                "org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode";
        public static final String NAME_SUFFIX = "_telemetry";
        public static final String NODE_ID_SEPARATOR = "|";
        public static final String SCRIPT_RESOURCE = "/rulechain/telemetry_to_kafka.js";
        public static final String SCRIPT_LANG_JS = "JS";
        public static final String JS_KIND_SCRIPT = "return metadata.tb_kind;";
        public static final String FETCH_TO_METADATA = "METADATA";
        public static final int FIRST_NODE_INDEX = 0;
        public static final int CONFIGURATION_VERSION = 1;
        public static final int MSG_TYPE_SWITCH_VERSION = 0;
        public static final int LAYOUT_START_X = 100;
        public static final int LAYOUT_START_Y = 100;
        public static final int LAYOUT_COLUMN_GAP = 300;
        public static final int LAYOUT_ROW_GAP = 120;
        public static final String CONFIG_LAYOUT_X = "layoutX";
        public static final String CONFIG_LAYOUT_Y = "layoutY";
        public static final int NODE_MSG_TYPE_SWITCH = 0;
        public static final int NODE_SAVE_TIMESERIES = 1;
        public static final int NODE_SAVE_CLIENT_ATTRIBUTES = 2;
        public static final int NODE_LOG_RPC_FROM_DEVICE = 3;
        public static final int NODE_LOG_OTHER = 4;
        public static final int NODE_SEND_RPC = 5;
        public static final int NODE_GET_ATTRIBUTES = 6;
        public static final int NODE_TRANSFORM = 7;
        public static final int NODE_SWITCH = 8;
        public static final int NODE_KAFKA_TELEMETRY = 9;
        public static final int NODE_KAFKA_EVENT = 10;
        public static final int NODE_KAFKA_INVALID = 11;
        public static final String NODE_KEY_MSG_TYPE_SWITCH = "msgTypeSwitch";
        public static final String NODE_KEY_SAVE_TIMESERIES = "saveTimeseries";
        public static final String NODE_KEY_SAVE_CLIENT_ATTRIBUTES = "saveClientAttributes";
        public static final String NODE_KEY_LOG_RPC_FROM_DEVICE = "logRpcFromDevice";
        public static final String NODE_KEY_LOG_OTHER = "logOther";
        public static final String NODE_KEY_SEND_RPC = "sendRpcRequest";
        public static final String NODE_KEY_GET_ATTRIBUTES = "getAttributes";
        public static final String NODE_KEY_TRANSFORM = "transform";
        public static final String NODE_KEY_SWITCH = "switch";
        public static final String NODE_KEY_KAFKA_TELEMETRY = "kafkaTelemetry";
        public static final String NODE_KEY_KAFKA_EVENT = "kafkaEvent";
        public static final String NODE_KEY_KAFKA_INVALID = "kafkaInvalid";
        public static final String NODE_NAME_MSG_TYPE_SWITCH = "Message Type Switch";
        public static final String NODE_NAME_SAVE_TIMESERIES = "Save Timeseries";
        public static final String NODE_NAME_SAVE_CLIENT_ATTRIBUTES = "Save Client Attributes";
        public static final String NODE_NAME_LOG_RPC_FROM_DEVICE = "Log RPC from Device";
        public static final String NODE_NAME_LOG_OTHER = "Log Other";
        public static final String NODE_NAME_SEND_RPC = "RPC Call Request";
        public static final String NODE_NAME_GET_ATTRIBUTES = "Get TSL Attributes";
        public static final String NODE_NAME_TRANSFORM = "TSL Validate And Normalize";
        public static final String NODE_NAME_SWITCH = "Route By Kind";
        public static final String NODE_NAME_KAFKA_TELEMETRY = "Kafka Telemetry";
        public static final String NODE_NAME_KAFKA_EVENT = "Kafka Event";
        public static final String NODE_NAME_KAFKA_INVALID = "Kafka Invalid";
        public static final String CONNECTION_POST_TELEMETRY = "Post telemetry";
        public static final String CONNECTION_POST_ATTRIBUTES = "Post attributes";
        public static final String CONNECTION_RPC_FROM_DEVICE = "RPC Request from Device";
        public static final String CONNECTION_RPC_TO_DEVICE = "RPC Request to Device";
        public static final String CONNECTION_OTHER = "Other";
        public static final String CONNECTION_SUCCESS = "Success";
        public static final String CONNECTION_TELEMETRY = "telemetry";
        public static final String CONNECTION_EVENT = "event";
        public static final String CONNECTION_INVALID = "invalid";
        public static final String CONFIG_VERSION = "version";
        public static final String CONFIG_FETCH_TO = "fetchTo";
        public static final String CONFIG_CLIENT_ATTRIBUTE_NAMES = "clientAttributeNames";
        public static final String CONFIG_SHARED_ATTRIBUTE_NAMES = "sharedAttributeNames";
        public static final String CONFIG_SERVER_ATTRIBUTE_NAMES = "serverAttributeNames";
        public static final String CONFIG_LATEST_TS_KEY_NAMES = "latestTsKeyNames";
        public static final String CONFIG_TELL_FAILURE_IF_ABSENT = "tellFailureIfAbsent";
        public static final String CONFIG_GET_LATEST_VALUE_WITH_TS = "getLatestValueWithTs";
        public static final String CONFIG_SCRIPT_LANG = "scriptLang";
        public static final String CONFIG_JS_SCRIPT = "jsScript";
        public static final String CONFIG_TBEL_SCRIPT = "tbelScript";
        public static final String CONFIG_TOPIC_PATTERN = "topicPattern";
        public static final String CONFIG_KEY_PATTERN = "keyPattern";
        public static final String CONFIG_BOOTSTRAP_SERVERS = "bootstrapServers";
        public static final String CONFIG_RETRIES = "retries";
        public static final String CONFIG_BATCH_SIZE = "batchSize";
        public static final String CONFIG_LINGER = "linger";
        public static final String CONFIG_BUFFER_MEMORY = "bufferMemory";
        public static final String CONFIG_ACKS = "acks";
        public static final String CONFIG_OTHER_PROPERTIES = "otherProperties";
        public static final String CONFIG_ADD_METADATA_AS_HEADERS = "addMetadataKeyValuesAsKafkaHeaders";
        public static final String CONFIG_HEADERS_CHARSET = "kafkaHeadersCharset";
        public static final String CONFIG_RULE_CHAIN_ID = "ruleChainId";
        public static final String CONFIG_FORWARD_TO_DEFAULT = "forwardMsgToDefaultRuleChain";
        public static final String CONFIG_DEFAULT_TTL = "defaultTTL";
        public static final String CONFIG_USE_SERVER_TS = "useServerTs";
        public static final String CONFIG_PROCESSING_SETTINGS = "processingSettings";
        public static final String CONFIG_PROCESSING_TYPE = "type";
        public static final String CONFIG_SCOPE = "scope";
        public static final String CONFIG_NOTIFY_DEVICE = "notifyDevice";
        public static final String CONFIG_SEND_ATTRIBUTES_UPDATED_NOTIFICATION =
                "sendAttributesUpdatedNotification";
        public static final String CONFIG_UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE =
                "updateAttributesOnlyOnValueChange";
        public static final String CONFIG_TIMEOUT_IN_SECONDS = "timeoutInSeconds";
        public static final String PROCESSING_TYPE_ON_EVERY_MESSAGE = "ON_EVERY_MESSAGE";
        public static final String SCOPE_CLIENT = "CLIENT_SCOPE";
        public static final String SCRIPT_LANG_TBEL = "TBEL";
        public static final String LOG_SCRIPT =
                "return '\\nIncoming message:\\n' + JSON.stringify(msg) + '\\nIncoming metadata:\\n' + JSON.stringify(metadata);";
        public static final int TIMESERIES_DEFAULT_TTL = 0;
        public static final int RPC_TIMEOUT_SECONDS = 60;
        public static final List<String> SERVER_ATTRIBUTE_NAMES = List.of(
                Product.ATTRIBUTE_TSL_SCHEMA,
                Product.ATTRIBUTE_TSL_VERSION,
                Product.ATTRIBUTE_TENANT_ID,
                Product.ATTRIBUTE_DEVICE_UUID,
                Product.ATTRIBUTE_SITE_CODE,
                Product.ATTRIBUTE_PRODUCT_ID);
    }

    public static final class Kafka {

        public static final int RETRIES = 3;
        public static final int BATCH_SIZE = 16384;
        public static final int LINGER = 1;
        public static final int BUFFER_MEMORY = 33554432;
        public static final String ACKS_ALL = "all";
        public static final String HEADERS_CHARSET_UTF8 = "UTF-8";
    }

    public static final class Db {

        public static final String CURRENT_DATABASE_SQL = "SELECT current_database(), current_schema()";
        public static final int DATABASE_COLUMN_INDEX = 1;
        public static final int SCHEMA_COLUMN_INDEX = 2;
        public static final String LATEST_ALIAS = "latest";
        public static final int LATEST_LIMIT = 1;
        public static final String ALIAS_TSL_VERSION = "tsl_version";
        public static final String ALIAS_TSL_STATUS = "tsl_status";
        public static final String ALIAS_PROPERTY_COUNT = "property_count";
        public static final String ALIAS_EVENT_COUNT = "event_count";
        public static final String ALIAS_ACTION_COUNT = "action_count";
        public static final String LIKE_PATTERN = "%%%s%%";
        public static final int NO_ROWS_AFFECTED = 0;
        public static final long ZERO_COUNT = 0L;
    }

    public static final class Defaults {

        public static final String TB_BASE_URL = "http://localhost:8080";
        public static final int TB_CLIENT_MAX_RETRIES = 3;
        public static final long TB_CLIENT_INITIAL_RETRY_DELAY_MS = 1000L;
        public static final long TB_CLIENT_MAX_RETRY_DELAY_MS = 30000L;
        public static final int AUTH_CACHE_SECONDS = 60;
        public static final String DB_NAME = "thingmodel";
        public static final String DB_SCHEMA = "";
        public static final String RULE_CHAIN_NAME_PREFIX = "auto_";
        public static final String KAFKA_TOPIC_TELEMETRY = "iot.telemetry.normalized";
        public static final String KAFKA_TOPIC_EVENT = "iot.event.normalized";
        public static final String KAFKA_TOPIC_INVALID = "iot.telemetry.invalid";
        public static final String KAFKA_KEY_PATTERN = "$[deviceName]";
    }

    public static final class ErrorCode {

        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String TENANT_ID_REQUIRED = "TENANT_ID_REQUIRED";
        public static final String TENANT_ID_INVALID = "TENANT_ID_INVALID";
        public static final String TENANT_REQUIRED = "TENANT_REQUIRED";
        public static final String ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
        public static final String FIELD_INVALID = "FIELD_INVALID";
        public static final String USERNAME_REQUIRED = "USERNAME_REQUIRED";
        public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
        public static final String TENANT_ACCOUNT_MISSING = "TENANT_ACCOUNT_MISSING";
        public static final String TENANT_ACCOUNT_DISABLED = "TENANT_ACCOUNT_DISABLED";
        public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
        public static final String PRODUCT_ID_CONFLICT = "PRODUCT_ID_CONFLICT";
        public static final String PRODUCT_CONFLICT = "PRODUCT_CONFLICT";
        public static final String PRODUCT_ID_MISSING = "PRODUCT_ID_MISSING";
        public static final String PRODUCT_ID_MISMATCH = "PRODUCT_ID_MISMATCH";
        public static final String TSL_REQUIRED = "TSL_REQUIRED";
        public static final String TSL_INVALID = "TSL_INVALID";
        public static final String TSL_NOT_FOUND = "TSL_NOT_FOUND";
        public static final String TSL_VERSION_EXISTS = "TSL_VERSION_EXISTS";
        public static final String VERSION_MISSING = "VERSION_MISSING";
        public static final String VERSION_MISMATCH = "VERSION_MISMATCH";
        public static final String TB_SYNC_FAILED = "TB_SYNC_FAILED";
        public static final String TB_UNAVAILABLE = "TB_UNAVAILABLE";
        public static final String TB_LOGIN_FAILED = "TB_LOGIN_FAILED";
        public static final String FUNCTION_POINTS = "FUNCTION_POINTS";
        public static final String ID_MISSING = "ID_MISSING";
        public static final String ID_DUP = "ID_DUP";
        public static final String ID_RESERVED = "ID_RESERVED";
        public static final String ID_INVALID = "ID_INVALID";
        public static final String MODE_INVALID = "MODE_INVALID";
        public static final String MODE_MISSING = "MODE_MISSING";
        public static final String EVENT_ID_MISSING = "EVENT_ID_MISSING";
        public static final String EVENT_ID_DUP = "EVENT_ID_DUP";
        public static final String EVENT_TYPE_VALUE = "EVENT_TYPE_VALUE";
        public static final String ACTION_ID_MISSING = "ACTION_ID_MISSING";
        public static final String ACTION_ID_DUP = "ACTION_ID_DUP";
        public static final String TYPE_MISSING = "TYPE_MISSING";
        public static final String TYPE_INVALID = "TYPE_INVALID";
        public static final String STRUCT_NOT_ALLOWED = "STRUCT_NOT_ALLOWED";
        public static final String RANGE_REVERSED = "RANGE_REVERSED";
        public static final String START_OUT_OF_RANGE = "START_OUT_OF_RANGE";
        public static final String STEP_INVALID = "STEP_INVALID";
        public static final String INT_RANGE = "INT_RANGE";
        public static final String ENUM_MAPPING = "ENUM_MAPPING";
        public static final String ENUM_KEY = "ENUM_KEY";
        public static final String MAPPING_NOT_ALLOWED = "MAPPING_NOT_ALLOWED";
        public static final String METADATA_TYPE_MISMATCH = "METADATA_TYPE_MISMATCH";
        public static final String STRUCT_EMPTY = "STRUCT_EMPTY";
        public static final String STRUCT_NESTED = "STRUCT_NESTED";
        public static final String PARAM_ID_MISSING = "PARAM_ID_MISSING";
        public static final String PARAM_DUP = "PARAM_DUP";
        public static final String PROFILE_MISSING = "PROFILE_MISSING";
        public static final String PROPERTIES_MISSING = "PROPERTIES_MISSING";
        public static final String DEFINE_MISSING = "DEFINE_MISSING";
        public static final String NAME_MISSING = "NAME_MISSING";
        public static final String FIELD_MISSING = "FIELD_MISSING";
        public static final String STRING_LENGTH = "STRING_LENGTH";
        public static final String RANGE_INVALID = "RANGE_INVALID";
        public static final String FIELD_UNKNOWN = "FIELD_UNKNOWN";
        public static final String SCHEMA_INVALID = "SCHEMA_INVALID";
    }
}
