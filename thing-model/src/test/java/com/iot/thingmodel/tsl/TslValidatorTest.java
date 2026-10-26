package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.thingmodel.common.Issue;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TslValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslValidator validator = new TslValidator(new TslParser(objectMapper));

    @Test
    void acceptsValidSampleAndReportsReservedKeyWarning() throws Exception {
        ValidationResult result = validator.validate(load("valid-tsl.json"));

        assertTrue(result.valid());
        assertTrue(codes(result.warnings()).contains("ID_RESERVED"));
        assertFalse(codes(result.errors()).contains("ID_INVALID"));
    }

    @Test
    void reportsDocumentedBusinessErrors() throws Exception {
        ValidationResult result = validator.validate(load("invalid-tsl.json"));
        Set<String> codes = codes(result.errors());

        assertTrue(codes.contains("ID_INVALID"));
        assertTrue(codes.contains("ID_DUP"));
        assertTrue(codes.contains("RANGE_REVERSED"));
        assertTrue(codes.contains("START_OUT_OF_RANGE"));
        assertTrue(codes.contains("ENUM_MAPPING"));
        assertTrue(codes.contains("STRUCT_NESTED"));
        assertTrue(codes.contains("EVENT_TYPE_VALUE"));
        assertTrue(codes.contains("PARAM_DUP"));
        assertTrue(codes.contains("STRUCT_NOT_ALLOWED"));
        assertTrue(codes(result.warnings()).contains("ID_RESERVED"));
    }

    @Test
    void limitsFunctionPointsToHundred() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("version", "1.0");
        root.putObject("profile").put("ProductId", "P-101");
        ArrayNode properties = root.putArray("properties");
        for (int i = 0; i < 101; i++) {
            ObjectNode property = properties.addObject();
            property.put("id", "p" + i);
            property.put("name", "p" + i);
            property.put("mode", "r");
            property.putObject("define").put("type", "int");
        }

        ValidationResult result = validator.validate(root);

        assertTrue(codes(result.errors()).contains("FUNCTION_POINTS"));
    }

    private JsonNode load(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/" + name)) {
            return objectMapper.readTree(input);
        }
    }

    private Set<String> codes(java.util.List<Issue> issues) {
        return issues.stream().map(Issue::code).collect(Collectors.toSet());
    }
}
