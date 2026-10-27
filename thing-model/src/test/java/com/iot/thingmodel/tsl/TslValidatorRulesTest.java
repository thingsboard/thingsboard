package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.Issue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TslValidatorRulesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslValidator validator = new TslValidator(new TslParser(objectMapper));

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTslCases")
    void reportsExpectedErrorCode(String name, String json, String expectedCode)
            throws Exception {
        ValidationResult result = validator.validate(objectMapper.readTree(json));

        assertTrue(result.errors().stream()
                .map(Issue::code)
                .collect(Collectors.toSet())
                .contains(expectedCode), name + " should contain " + expectedCode
                + " but got " + result.errors());
    }

    @Test
    void reportsReservedKeyAsWarning() throws Exception {
        ValidationResult result = validator.validate(objectMapper.readTree("""
                {
                  "version": "1.0",
                  "profile": {"ProductId": "P1"},
                  "properties": [{
                    "id": "event_id",
                    "name": "Reserved",
                    "mode": "r",
                    "define": {"type": "string"}
                  }]
                }
                """));

        assertTrue(result.warnings().stream()
                .map(Issue::code)
                .collect(Collectors.toSet())
                .contains("ID_RESERVED"));
    }

    private static Stream<Arguments> invalidTslCases() {
        return Stream.of(
                Arguments.of("missing version", """
                        {"profile":{"ProductId":"P1"},"properties":[]}
                        """, "VERSION_MISSING"),
                Arguments.of("missing product id", """
                        {"version":"1.0","profile":{},"properties":[]}
                        """, "PRODUCT_ID_MISSING"),
                Arguments.of("invalid id", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"1speed","name":"Speed","mode":"r","define":{"type":"int"}}]}
                        """, "ID_INVALID"),
                Arguments.of("duplicate id", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"A","mode":"r","define":{"type":"int"}},
                          {"id":"p1","name":"B","mode":"r","define":{"type":"int"}}]}
                        """, "ID_DUP"),
                Arguments.of("invalid mode", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"x","define":{"type":"int"}}]}
                        """, "MODE_INVALID"),
                Arguments.of("missing type", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{}}]}
                        """, "TYPE_MISSING"),
                Arguments.of("invalid type", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"date"}}]}
                        """, "TYPE_INVALID"),
                Arguments.of("reversed range", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"int","min":10,"max":0}}]}
                        """, "RANGE_REVERSED"),
                Arguments.of("start out of range", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"int","min":0,"max":10,"start":20}}]}
                        """, "START_OUT_OF_RANGE"),
                Arguments.of("negative step", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"int","step":-1}}]}
                        """, "STEP_INVALID"),
                Arguments.of("int range", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"int","min":-3000000000}}]}
                        """, "INT_RANGE"),
                Arguments.of("enum mapping too small", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"enum","mapping":{"0":"A"}}}]}
                        """, "ENUM_MAPPING"),
                Arguments.of("enum key not numeric", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"enum","mapping":{"a":"A","b":"B"}}}]}
                        """, "ENUM_KEY"),
                Arguments.of("empty struct", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"struct","specs":{}}}]}
                        """, "STRUCT_EMPTY"),
                Arguments.of("nested struct", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r",
                           "define":{"type":"struct","specs":{"x":{"type":"struct"}}}}]}
                        """, "STRUCT_NESTED"),
                Arguments.of("struct event param", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"int"}}],
                         "events":[{"id":"e1","name":"E1","type":"info","params":[
                          {"id":"x","name":"X","define":{"type":"struct","specs":{"y":{"type":"int"}}}}]}]}
                        """, "STRUCT_NOT_ALLOWED"),
                Arguments.of("duplicate event id", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"int"}}],
                         "events":[
                          {"id":"e1","name":"E1","type":"info"},
                          {"id":"e1","name":"E2","type":"alert"}]}
                        """, "EVENT_ID_DUP"),
                Arguments.of("invalid event type", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"int"}}],
                         "events":[{"id":"e1","name":"E1","type":"warn"}]}
                        """, "EVENT_TYPE_VALUE"),
                Arguments.of("duplicate action id", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"int"}}],
                         "actions":[
                          {"id":"a1","name":"A1"},
                          {"id":"a1","name":"A2"}]}
                        """, "ACTION_ID_DUP"),
                Arguments.of("missing param id", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"int"}}],
                         "events":[{"id":"e1","name":"E1","type":"info","params":[
                          {"name":"X","define":{"type":"int"}}]}]}
                        """, "PARAM_ID_MISSING"),
                Arguments.of("duplicate param", """
                        {"version":"1.0","profile":{"ProductId":"P1"},"properties":[
                          {"id":"p1","name":"P1","mode":"r","define":{"type":"int"}}],
                         "events":[{"id":"e1","name":"E1","type":"info","params":[
                          {"id":"x","name":"X","define":{"type":"int"}},
                          {"id":"x","name":"Y","define":{"type":"int"}}]}]}
                        """, "PARAM_DUP")
        );
    }
}
