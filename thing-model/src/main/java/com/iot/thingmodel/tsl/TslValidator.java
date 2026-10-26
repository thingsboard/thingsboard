package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.thingmodel.common.Issue;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

import static com.iot.thingmodel.common.Constants.ErrorCode.ACTION_ID_DUP;
import static com.iot.thingmodel.common.Constants.ErrorCode.ACTION_ID_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.DEFINE_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.ENUM_KEY;
import static com.iot.thingmodel.common.Constants.ErrorCode.ENUM_MAPPING;
import static com.iot.thingmodel.common.Constants.ErrorCode.EVENT_ID_DUP;
import static com.iot.thingmodel.common.Constants.ErrorCode.EVENT_ID_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.EVENT_TYPE_VALUE;
import static com.iot.thingmodel.common.Constants.ErrorCode.FIELD_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.FIELD_UNKNOWN;
import static com.iot.thingmodel.common.Constants.ErrorCode.FUNCTION_POINTS;
import static com.iot.thingmodel.common.Constants.ErrorCode.ID_DUP;
import static com.iot.thingmodel.common.Constants.ErrorCode.ID_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.ID_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.ID_RESERVED;
import static com.iot.thingmodel.common.Constants.ErrorCode.INT_RANGE;
import static com.iot.thingmodel.common.Constants.ErrorCode.MAPPING_NOT_ALLOWED;
import static com.iot.thingmodel.common.Constants.ErrorCode.METADATA_TYPE_MISMATCH;
import static com.iot.thingmodel.common.Constants.ErrorCode.MODE_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.MODE_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.NAME_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.PARAM_DUP;
import static com.iot.thingmodel.common.Constants.ErrorCode.PARAM_ID_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.PRODUCT_ID_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.PROFILE_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.PROPERTIES_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.RANGE_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.RANGE_REVERSED;
import static com.iot.thingmodel.common.Constants.ErrorCode.SCHEMA_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.START_OUT_OF_RANGE;
import static com.iot.thingmodel.common.Constants.ErrorCode.STEP_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.STRING_LENGTH;
import static com.iot.thingmodel.common.Constants.ErrorCode.STRUCT_EMPTY;
import static com.iot.thingmodel.common.Constants.ErrorCode.STRUCT_NESTED;
import static com.iot.thingmodel.common.Constants.ErrorCode.STRUCT_NOT_ALLOWED;
import static com.iot.thingmodel.common.Constants.ErrorCode.TSL_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.TYPE_INVALID;
import static com.iot.thingmodel.common.Constants.ErrorCode.TYPE_MISSING;
import static com.iot.thingmodel.common.Constants.ErrorCode.VERSION_MISSING;
import static com.iot.thingmodel.common.Constants.Tsl.EVENT_TYPES;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_COMPACT_KEY;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_DEFINE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MODE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_NAME;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PROFILE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_SPECS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_TYPE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_VERSION;
import static com.iot.thingmodel.common.Constants.Tsl.MODES;
import static com.iot.thingmodel.common.Constants.Tsl.NUMERIC_TYPES;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_ACTIONS;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_DEFINE;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_EVENTS;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_ID;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_INPUT;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_MAPPING;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_MODE;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_NAME;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_OUTPUT;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_PARAMS;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_PROFILE;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_PROPERTIES;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_PROPERTIES_ROOT;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_PROFILE_PRODUCT_ID;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_ROOT;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_SPECS;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_START;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_STEP;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_TYPE;
import static com.iot.thingmodel.common.Constants.Tsl.PATH_VERSION;
import static com.iot.thingmodel.common.Constants.Tsl.RESERVED_KEYS;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_ADDITIONAL_PROPERTIES;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_ENUM;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_EXCLUSIVE_MAXIMUM;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_EXCLUSIVE_MINIMUM;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_MAXIMUM;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_MAX_LENGTH;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_MINIMUM;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_PATTERN;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_REQUIRED;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_KEYWORD_TYPE;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_RESOURCE;
import static com.iot.thingmodel.common.Constants.Tsl.TYPE_ENUM;
import static com.iot.thingmodel.common.Constants.Tsl.TYPE_ENUM_STRING;
import static com.iot.thingmodel.common.Constants.Tsl.TYPE_INT;
import static com.iot.thingmodel.common.Constants.Tsl.TYPE_STRUCT;
import static com.iot.thingmodel.common.Constants.Tsl.TYPES;
import static com.iot.thingmodel.common.Constants.Validation.IDENTIFIER_PATTERN;
import static com.iot.thingmodel.common.Constants.Validation.ISSUE_KEY_SEPARATOR;
import static com.iot.thingmodel.common.Constants.Validation.MAX_FUNCTION_POINTS;
import static com.iot.thingmodel.common.Constants.Validation.MIN_ENUM_MAPPING_SIZE;

@Component
public class TslValidator {

    private static final double INT_MIN = Integer.MIN_VALUE;
    private static final double INT_MAX = Integer.MAX_VALUE;

    private final TslParser parser;
    private final JsonSchema schema;

    public TslValidator(TslParser parser) {
        this.parser = parser;
        this.schema = loadSchema();
    }

    public ValidationResult validate(JsonNode root) {
        List<Issue> errors = new ArrayList<>();
        List<Issue> warnings = new ArrayList<>();
        Set<String> seenErrors = new HashSet<>();
        Set<String> seenWarnings = new HashSet<>();

        if (root == null || root.isNull() || !root.isObject()) {
            errors.add(new Issue(TSL_INVALID, PATH_ROOT, "物模型必须是 JSON 对象"));
            return new ValidationResult(errors, warnings);
        }

        for (ValidationMessage message : schema.validate(root)) {
            Issue issue = map(message);
            add(errors, seenErrors, issue);
        }

        TslDefinition definition = parser.parse(root);
        validateBusiness(definition, errors, warnings, seenErrors, seenWarnings);
        return new ValidationResult(errors, warnings);
    }

    private void validateBusiness(
            TslDefinition definition,
            List<Issue> errors,
            List<Issue> warnings,
            Set<String> seenErrors,
            Set<String> seenWarnings) {
        if (isBlank(definition.version())) {
            add(errors, seenErrors, new Issue(VERSION_MISSING, PATH_VERSION, "缺少物模型版本"));
        }
        if (isBlank(definition.productId())) {
            add(errors, seenErrors, new Issue(
                    PRODUCT_ID_MISSING, PATH_PROFILE_PRODUCT_ID, "缺少 ProductId"));
        }

        int functionPoints = definition.properties().size()
                + definition.events().size()
                + definition.actions().size();
        if (functionPoints > MAX_FUNCTION_POINTS) {
            add(errors, seenErrors, new Issue(
                    FUNCTION_POINTS,
                    PATH_ROOT,
                    "属性、事件、行为总数不能超过 " + MAX_FUNCTION_POINTS));
        }

        Set<String> propertyIds = new HashSet<>();
        for (int i = 0; i < definition.properties().size(); i++) {
            TslProperty property = definition.properties().get(i);
            String path = PATH_PROPERTIES + i;
            validateId(property.id(), path, ID_MISSING, errors, seenErrors);
            if (property.id() != null) {
                if (!propertyIds.add(property.id())) {
                    add(errors, seenErrors, new Issue(
                            ID_DUP, path + PATH_ID, "属性标识重复: " + property.id()));
                }
                if (RESERVED_KEYS.contains(property.id())) {
                    add(warnings, seenWarnings, new Issue(
                            ID_RESERVED, path + PATH_ID, "属性使用了平台保留键: " + property.id()));
                }
            }
            if (property.mode() != null && !MODES.contains(property.mode())) {
                add(errors, seenErrors, new Issue(
                        MODE_INVALID, path + PATH_MODE, "属性模式必须是 r 或 rw"));
            }
            validateDefine(
                    property.define(),
                    path + PATH_DEFINE,
                    false,
                    errors,
                    warnings,
                    seenErrors,
                    seenWarnings);
        }

        Set<String> eventIds = new HashSet<>();
        for (int i = 0; i < definition.events().size(); i++) {
            TslEvent event = definition.events().get(i);
            String path = PATH_EVENTS + i;
            validateId(event.id(), path, EVENT_ID_MISSING, errors, seenErrors);
            if (event.id() != null && !eventIds.add(event.id())) {
                add(errors, seenErrors, new Issue(
                        EVENT_ID_DUP, path + PATH_ID, "事件标识重复: " + event.id()));
            }
            if (event.type() != null && !EVENT_TYPES.contains(event.type())) {
                add(errors, seenErrors, new Issue(
                        EVENT_TYPE_VALUE,
                        path + PATH_TYPE,
                        "事件类型必须是 info、alert 或 fault"));
            }
            validateParams(event.params(), path + PATH_PARAMS, errors, seenErrors, true);
        }

        Set<String> actionIds = new HashSet<>();
        for (int i = 0; i < definition.actions().size(); i++) {
            TslAction action = definition.actions().get(i);
            String path = PATH_ACTIONS + i;
            validateId(action.id(), path, ACTION_ID_MISSING, errors, seenErrors);
            if (action.id() != null && !actionIds.add(action.id())) {
                add(errors, seenErrors, new Issue(
                        ACTION_ID_DUP, path + PATH_ID, "行为标识重复: " + action.id()));
            }
            validateParams(action.input(), path + PATH_INPUT, errors, seenErrors, false);
            validateParams(action.output(), path + PATH_OUTPUT, errors, seenErrors, false);
        }
    }

    private void validateDefine(
            TslDefine define,
            String path,
            boolean paramContext,
            List<Issue> errors,
            List<Issue> warnings,
            Set<String> seenErrors,
            Set<String> seenWarnings) {
        if (define == null || isBlank(define.type())) {
            add(errors, seenErrors, new Issue(
                    TYPE_MISSING, path + PATH_TYPE, "缺少数据类型"));
            return;
        }
        if (!TYPES.contains(define.type())) {
            add(errors, seenErrors, new Issue(
                    TYPE_INVALID, path + PATH_TYPE, "不支持的数据类型: " + define.type()));
            return;
        }
        if (paramContext && TYPE_STRUCT.equals(define.type())) {
            add(errors, seenErrors, new Issue(
                    STRUCT_NOT_ALLOWED,
                    path + PATH_TYPE,
                    "事件或行为参数不允许使用结构体"));
        }

        Double min = define.min();
        Double max = define.max();
        Double start = define.start();
        Double step = define.step();

        if (min != null && max != null && min > max) {
            add(errors, seenErrors, new Issue(RANGE_REVERSED, path, "min 不能大于 max"));
        }
        if (start != null && min != null && start < min) {
            add(errors, seenErrors, new Issue(
                    START_OUT_OF_RANGE, path + PATH_START, "start 小于 min"));
        }
        if (start != null && max != null && start > max) {
            add(errors, seenErrors, new Issue(
                    START_OUT_OF_RANGE, path + PATH_START, "start 大于 max"));
        }
        if (step != null && step < 0) {
            add(errors, seenErrors, new Issue(
                    STEP_INVALID, path + PATH_STEP, "step 不能为负数"));
        }
        if (TYPE_INT.equals(define.type()) && outsideIntRange(min, max, start)) {
            add(errors, seenErrors, new Issue(
                    INT_RANGE, path, "int 类型的取值范围不能超过 32 位"));
        }

        if (TYPE_ENUM.equals(define.type()) || TYPE_ENUM_STRING.equals(define.type())) {
            JsonNode mapping = define.mapping();
            int size = mappingSize(mapping);
            if (size < MIN_ENUM_MAPPING_SIZE) {
                add(errors, seenErrors, new Issue(
                        ENUM_MAPPING,
                        path + PATH_MAPPING,
                        "枚举至少需要 " + MIN_ENUM_MAPPING_SIZE + " 项 mapping"));
            }
            if (TYPE_ENUM.equals(define.type()) && !mappingKeysNumeric(mapping)) {
                add(errors, seenErrors, new Issue(
                        ENUM_KEY, path + PATH_MAPPING, "枚举 mapping 的键必须是数字"));
            }
        } else if (define.mapping() != null && !define.mapping().isNull()) {
            add(warnings, seenWarnings, new Issue(
                    MAPPING_NOT_ALLOWED,
                    path + PATH_MAPPING,
                    "非枚举类型不应配置 mapping"));
        }

        if (TYPE_STRUCT.equals(define.type())) {
            validateStruct(define.specs(), path, errors, seenErrors);
        }

        if (min != null || max != null || start != null || step != null) {
            if (!NUMERIC_TYPES.contains(define.type())) {
                add(warnings, seenWarnings, new Issue(
                        METADATA_TYPE_MISMATCH, path, "该类型不应配置数值范围"));
            }
        }
    }

    private void validateStruct(
            JsonNode specs,
            String path,
            List<Issue> errors,
            Set<String> seenErrors) {
        int size = specs == null || specs.isNull() ? 0 : specs.size();
        if (size == 0) {
            add(errors, seenErrors, new Issue(
                    STRUCT_EMPTY, path + PATH_SPECS, "结构体字段不能为空"));
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = specs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode field = entry.getValue();
            String fieldType = field.path(KEY_TYPE).asText(null);
            if (fieldType == null) {
                fieldType = field.path(KEY_DEFINE).path(KEY_TYPE).asText(null);
            }
            if (TYPE_STRUCT.equals(fieldType)) {
                add(errors, seenErrors, new Issue(
                        STRUCT_NESTED,
                        path + PATH_SPECS + PATH_ROOT + entry.getKey(),
                        "结构体不允许嵌套结构体"));
            }
        }
    }

    private void validateParams(
            List<TslParam> params,
            String path,
            List<Issue> errors,
            Set<String> seenErrors,
            boolean paramContext) {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < params.size(); i++) {
            TslParam param = params.get(i);
            String paramPath = path + PATH_ROOT + i;
            validateId(param.id(), paramPath, PARAM_ID_MISSING, errors, seenErrors);
            if (param.id() != null && !ids.add(param.id())) {
                add(errors, seenErrors, new Issue(
                        PARAM_DUP, paramPath + PATH_ID, "参数标识重复: " + param.id()));
            }
            validateDefine(
                    param.define(),
                    paramPath + PATH_DEFINE,
                    true,
                    errors,
                    List.of(),
                    seenErrors,
                    new HashSet<>());
        }
    }

    private void validateId(
            String id,
            String path,
            String missingCode,
            List<Issue> errors,
            Set<String> seenErrors) {
        if (isBlank(id)) {
            add(errors, seenErrors, new Issue(missingCode, path + PATH_ID, "缺少标识符"));
        } else if (!IDENTIFIER_PATTERN.matcher(id).matches()) {
            add(errors, seenErrors, new Issue(
                    ID_INVALID, path + PATH_ID, "标识符格式不合法: " + id));
        }
    }

    private Issue map(ValidationMessage message) {
        String code = message.getCode();
        String path = message.getInstanceLocation() == null
                ? PATH_ROOT
                : message.getInstanceLocation().toString();
        String mapped = switch (code == null ? "" : code) {
            case SCHEMA_KEYWORD_REQUIRED -> missingCode(path);
            case SCHEMA_KEYWORD_TYPE -> TYPE_INVALID;
            case SCHEMA_KEYWORD_ENUM -> path.endsWith(PATH_TYPE) || path.contains(PATH_EVENTS)
                    ? EVENT_TYPE_VALUE
                    : TYPE_INVALID;
            case SCHEMA_KEYWORD_PATTERN -> ID_INVALID;
            case SCHEMA_KEYWORD_MAX_LENGTH -> STRING_LENGTH;
            case SCHEMA_KEYWORD_MINIMUM,
                    SCHEMA_KEYWORD_MAXIMUM,
                    SCHEMA_KEYWORD_EXCLUSIVE_MINIMUM,
                    SCHEMA_KEYWORD_EXCLUSIVE_MAXIMUM -> RANGE_INVALID;
            case SCHEMA_KEYWORD_ADDITIONAL_PROPERTIES -> FIELD_UNKNOWN;
            default -> SCHEMA_INVALID;
        };
        return new Issue(mapped, path, message.getMessage());
    }

    private String missingCode(String path) {
        if (path.contains(PATH_PROFILE)) {
            return PROFILE_MISSING;
        }
        if (path.endsWith(PATH_ID)) {
            return ID_MISSING;
        }
        if (path.endsWith(PATH_VERSION)) {
            return VERSION_MISSING;
        }
        if (path.endsWith(PATH_PROPERTIES_ROOT)) {
            return PROPERTIES_MISSING;
        }
        if (path.endsWith(PATH_DEFINE)) {
            return DEFINE_MISSING;
        }
        if (path.endsWith(PATH_TYPE)) {
            return TYPE_MISSING;
        }
        if (path.endsWith(PATH_NAME)) {
            return NAME_MISSING;
        }
        if (path.endsWith(PATH_MODE)) {
            return MODE_MISSING;
        }
        return FIELD_MISSING;
    }

    private int mappingSize(JsonNode mapping) {
        if (mapping == null || mapping.isNull()) {
            return 0;
        }
        if (mapping.isObject()) {
            return mapping.size();
        }
        if (mapping.isArray()) {
            return mapping.size();
        }
        return 0;
    }

    private boolean mappingKeysNumeric(JsonNode mapping) {
        if (mapping == null || mapping.isNull()) {
            return false;
        }
        if (mapping.isObject()) {
            Iterator<String> names = mapping.fieldNames();
            while (names.hasNext()) {
                if (!isNumeric(names.next())) {
                    return false;
                }
            }
            return true;
        }
        if (mapping.isArray()) {
            for (JsonNode item : mapping) {
                JsonNode key = item.get(KEY_COMPACT_KEY);
                if (key == null) {
                    key = item.get(KEY_ID);
                }
                if (key == null || !isNumeric(key.asText())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean outsideIntRange(Double min, Double max, Double start) {
        return outsideIntRange(min) || outsideIntRange(max) || outsideIntRange(start);
    }

    private boolean outsideIntRange(Double value) {
        return value != null && (value < INT_MIN || value > INT_MAX);
    }

    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void add(List<Issue> issues, Set<String> seen, Issue issue) {
        String key = issue.code() + ISSUE_KEY_SEPARATOR + issue.path();
        if (seen.add(key)) {
            issues.add(issue);
        }
    }

    private JsonSchema loadSchema() {
        try (InputStream input = TslValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + SCHEMA_RESOURCE);
            }
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            return factory.getSchema(input);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load TSL JSON schema", ex);
        }
    }
}
